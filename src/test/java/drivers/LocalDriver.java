package drivers;

import com.codeborne.selenide.WebDriverProvider;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static config.Project.localConfig;
import static io.appium.java_client.remote.AutomationName.ANDROID_UIAUTOMATOR2;
import static io.appium.java_client.remote.MobilePlatform.ANDROID;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

/**
 * Создаёт для Selenide сессию на локальном Appium-сервере: эмулятор или телефон в adb.
 * Подключается через {@code Configuration.browser = LocalDriver.class.getName()}.
 * Настройки — в {@code local.properties}, см. {@link config.LocalConfig}.
 */
public class LocalDriver implements WebDriverProvider {

    private static final String USER_AGENT =
            "qa-guru-course-mobile/1.0 (https://github.com/Dmitry5401/qa_guru_course_mobile)";

    @Nonnull
    @Override
    public WebDriver createDriver(@Nonnull Capabilities capabilities) {
        URL appiumUrl = getAppiumServerUrl();
        // Проверяем сервер до скачивания APK: SessionNotCreatedException про недоступный
        // адрес читается плохо, а ждать загрузки приложения ради него совсем ни к чему.
        checkAppiumIsReachable(appiumUrl);

        UiAutomator2Options options = new UiAutomator2Options();
        options.merge(capabilities);

        options.setAutomationName(ANDROID_UIAUTOMATOR2)
                .setPlatformName(ANDROID)
                .setApp(getAppPath())
                .setAppPackage(localConfig.appPackage())
                .setAppActivity(localConfig.appActivity());

        // deviceName Appium при выборе устройства игнорирует: сначала смотрит udid,
        // затем platformVersion, иначе берёт первое устройство из adb devices.
        if (!localConfig.deviceUdid().isBlank()) {
            options.setUdid(localConfig.deviceUdid());
        } else if (!localConfig.platformVersion().isBlank()) {
            options.setPlatformVersion(localConfig.platformVersion());
        }

        return new AndroidDriver(appiumUrl, options);
    }

    public static URL getAppiumServerUrl() {
        try {
            return URI.create(localConfig.appiumUrl()).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Некорректный адрес Appium: " + localConfig.appiumUrl(), e);
        }
    }

    private static final String LEGACY_BASE_PATH = "/wd/hub";

    private enum Probe { OK, NO_ROUTE, UNREACHABLE }

    private void checkAppiumIsReachable(URL appiumUrl) {
        String base = appiumUrl.toString().replaceAll("/+$", "");
        Probe probe = probeStatus(base);
        if (probe == Probe.OK) {
            return;
        }
        if (probe == Probe.UNREACHABLE) {
            throw new IllegalStateException("Appium не отвечает на " + base + "/status."
                    + " Запустите сервер командой `appium` и сверьте адрес в LOCAL_APPIUM_URL.");
        }

        // Роут не найден. Чаще всего базовый путь сервера и LOCAL_APPIUM_URL просто
        // разъехались, поэтому проверяем второй вариант и называем рабочий адрес.
        String alternative = base.endsWith(LEGACY_BASE_PATH)
                ? base.substring(0, base.length() - LEGACY_BASE_PATH.length())
                : base + LEGACY_BASE_PATH;

        if (probeStatus(alternative) == Probe.OK) {
            throw new IllegalStateException("Appium слушает " + alternative
                    + ", а LOCAL_APPIUM_URL указывает на " + base + "."
                    + " Либо поставьте LOCAL_APPIUM_URL=" + alternative + "/,"
                    + " либо перезапустите сервер с другим --base-path."
                    + " У Appium 2 и 3 базовый путь по умолчанию — корень,"
                    + " префикс " + LEGACY_BASE_PATH + " остался в Appium 1.");
        }

        throw new IllegalStateException("Appium отвечает, но роута /status нет ни на " + base
                + ", ни на " + alternative + ". Проверьте --base-path, с которым запущен сервер.");
    }

    private Probe probeStatus(String base) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/status"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        try {
            int code = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            return code == 404 ? Probe.NO_ROUTE : Probe.OK;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Прервана проверка Appium на " + base, e);
        } catch (IOException e) {
            return Probe.UNREACHABLE;
        }
    }

    private String getAppPath() {
        String appUrl = localConfig.appUrl();
        String appFileName = appUrl.substring(appUrl.lastIndexOf('/') + 1);
        // Не в build/ и не в resources: clean не должен стирать скачанный APK,
        // а processTestResources — копировать его на каждую сборку.
        File app = new File("apps/" + appFileName);

        if (!app.exists()) {
            try {
                URLConnection connection = URI.create(appUrl).toURL().openConnection();
                // releases.wikimedia.org отдаёт 403 на дефолтный User-Agent вида Java/21:
                // по их политике клиент должен представляться. https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy
                connection.setRequestProperty("User-Agent", USER_AGENT);
                try (InputStream in = connection.getInputStream()) {
                    copyInputStreamToFile(in, app);
                }
            } catch (IOException e) {
                throw new AssertionError("Не удалось скачать приложение: " + appUrl, e);
            }
        }
        return app.getAbsolutePath();
    }
}
