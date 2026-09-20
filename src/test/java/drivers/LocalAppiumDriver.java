package drivers;

import com.codeborne.selenide.WebDriverProvider;
import config.LocalStandConfig;
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
 * <p>
 * Тексты исключений здесь на английском намеренно: Gradle печатает их в консоль в UTF-8,
 * не спрашивая кодовую страницу терминала, и на русской Windows (OEM 866) русский текст
 * превращается в «╤Б╨╗╤Г╤И╨░╨╡╤В». Комментарии и шаги Allure читаются в IDE и в отчёте,
 * там с UTF-8 проблем нет, поэтому они остаются на русском.
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

        // deviceName Appium при выборе устройства игнорирует. Порядок такой: avd, затем
        // udid, затем platformVersion, иначе первое устройство из adb devices.
        // avd удобен для эмулятора: имя из Device Manager стабильно, а номер порта
        // в emulator-5554 зависит от порядка запуска. Вдобавок Appium сам поднимет
        // эмулятор с таким именем, если тот ещё не запущен.
        if (!localConfig.avd().isBlank()) {
            options.setAvd(localConfig.avd());
        } else if (!localConfig.deviceUdid().isBlank()) {
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
            throw new IllegalStateException("Malformed Appium URL: " + localConfig.appiumUrl(), e);
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
            throw new IllegalStateException("Appium does not answer at " + base + "/status."
                    + " Start the server with `appium` and check LOCAL_APPIUM_URL.");
        }

        // Роут не найден. Чаще всего базовый путь сервера и LOCAL_APPIUM_URL просто
        // разъехались, поэтому проверяем второй вариант и называем рабочий адрес.
        String alternative = base.endsWith(LEGACY_BASE_PATH)
                ? base.substring(0, base.length() - LEGACY_BASE_PATH.length())
                : base + LEGACY_BASE_PATH;

        if (probeStatus(alternative) == Probe.OK) {
            throw new IllegalStateException("Appium listens at " + alternative
                    + ", but LOCAL_APPIUM_URL points to " + base + "."
                    + " Fix either side: put LOCAL_APPIUM_URL=" + alternative + "/"
                    + " into local.properties, or restart the server so it serves " + base + "."
                    + " Appium 2 and 3 serve the root path by default,"
                    + " the " + LEGACY_BASE_PATH + " prefix is left over from Appium 1.");
        }

        throw new IllegalStateException("Appium answers, but there is no /status route at " + base
                + " nor at " + alternative + ". Check the --base-path the server was started with.");
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
            throw new IllegalStateException("Interrupted while probing Appium at " + base, e);
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
                throw new AssertionError("Failed to download the app: " + appUrl, e);
            }
        }
        return app.getAbsolutePath();
    }
}
