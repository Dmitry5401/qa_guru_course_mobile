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

import static io.appium.java_client.remote.AutomationName.ANDROID_UIAUTOMATOR2;
import static io.appium.java_client.remote.MobilePlatform.ANDROID;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

/**
 * Общая часть стендов, работающих через Appium на своей машине: {@link EmulationDriver}
 * и {@link RealDriver}. Сервер, приложение и проверки у них одни и те же, различается
 * только выбор устройства — его задаёт наследник в {@link #selectDevice}.
 * <p>
 * Тексты исключений здесь на английском намеренно: Gradle печатает их в консоль в UTF-8,
 * не спрашивая кодовую страницу терминала, и на русской Windows (OEM 866) русский текст
 * превращается в «╤Б╨╗╤Г╤И╨░╨╡╤В». Комментарии и шаги Allure читаются в IDE и в отчёте,
 * там с UTF-8 проблем нет, поэтому они остаются на русском.
 */
public abstract class LocalAppiumDriver implements WebDriverProvider {

    private static final String USER_AGENT =
            "qa-guru-course-mobile/1.0 (https://github.com/Dmitry5401/qa_guru_course_mobile)";
    private static final String LEGACY_BASE_PATH = "/wd/hub";

    /** Сколько ждём приложение на экране после старта сессии. */
    private static final Duration APP_START_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration FOREGROUND_POLL_INTERVAL = Duration.ofMillis(500);

    private enum Probe { OK, NO_ROUTE, UNREACHABLE }

    /** Настройки стенда: адрес сервера и приложение. */
    protected abstract LocalStandConfig config();

    /**
     * Указывает Appium, на какое устройство идти. {@code deviceName} для этого не годится:
     * Appium смотрит на {@code avd}, затем на {@code udid}, а если не задано ничего —
     * берёт первое устройство из {@code adb devices}.
     */
    protected abstract void selectDevice(UiAutomator2Options options);

    @Nonnull
    @Override
    public WebDriver createDriver(@Nonnull Capabilities capabilities) {
        URL appiumUrl = appiumServerUrl();
        // Проверяем сервер до скачивания APK: SessionNotCreatedException про недоступный
        // адрес читается плохо, а ждать загрузки приложения ради него совсем ни к чему.
        checkAppiumIsReachable(appiumUrl);

        UiAutomator2Options options = new UiAutomator2Options();
        options.merge(capabilities);

        options.setAutomationName(ANDROID_UIAUTOMATOR2)
                .setPlatformName(ANDROID)
                .setApp(appPath())
                .setAppPackage(config().appPackage())
                .setAppActivity(config().appActivity());

        selectDevice(options);
        selectLanguage(options);

        AndroidDriver driver = new AndroidDriver(appiumUrl, options);
        checkAppIsOnScreen(driver);
        return driver;
    }

    /**
     * Проверяет, что приложение действительно на экране.
     * <p>
     * Сессия создаётся успешно и тогда, когда приложение с экрана уже ушло: язык стенда
     * Appium меняет через системные настройки, а смена языка перезапускает приложения, и
     * телефон может остаться на рабочем столе. Без этой проверки тест тридцать секунд
     * ищет кнопку на чужом экране и падает с «Element not found», как будто виноват
     * локатор.
     * <p>
     * Заблокированный телефон — второй такой случай и единственный, который не лечится
     * кодом: через защищённый экран блокировки Appium не проходит.
     */
    private void checkAppIsOnScreen(AndroidDriver driver) {
        String appPackage = config().appPackage();
        if (waitForForeground(driver, appPackage)) {
            return;
        }
        if (driver.isDeviceLocked()) {
            throw new IllegalStateException("The device is locked, so " + appPackage
                    + " never made it to the screen. Appium does not get through a secure lock"
                    + " screen on its own: unlock the phone, keep the screen awake, and run again.");
        }

        driver.activateApp(appPackage);
        if (waitForForeground(driver, appPackage)) {
            return;
        }

        throw new IllegalStateException("The screen belongs to " + driver.getCurrentPackage()
                + " (" + driver.currentActivity() + "), not to " + appPackage + "."
                + " Start the app on the device by hand to see what happens,"
                + " and check LOCAL_APP_PACKAGE and LOCAL_APP_ACTIVITY.");
    }

    private boolean waitForForeground(AndroidDriver driver, String appPackage) {
        long deadline = System.currentTimeMillis() + APP_START_TIMEOUT.toMillis();
        while (true) {
            if (appPackage.equals(driver.getCurrentPackage())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(FOREGROUND_POLL_INTERVAL);
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the app to start", e);
        }
    }

    /**
     * Переводит устройство в язык стенда — от него зависит язык статей в приложении,
     * а его сверяет {@code ArticleTests}. См. {@link LocalStandConfig#language()}.
     */
    private void selectLanguage(UiAutomator2Options options) {
        // Пустое значение не проставляем: Appium в этом случае оставит язык устройства
        // в покое, а иначе увёл бы его в пустую локаль.
        if (!config().language().isBlank()) {
            options.setLanguage(config().language());
            options.setLocale(config().locale());
        }
    }

    private URL appiumServerUrl() {
        try {
            return URI.create(config().appiumUrl()).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Malformed Appium URL: " + config().appiumUrl(), e);
        }
    }

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

    private String appPath() {
        String appUrl = config().appUrl();
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
