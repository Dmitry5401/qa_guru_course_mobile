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

        return new AndroidDriver(getAppiumServerUrl(), options);
    }

    public static URL getAppiumServerUrl() {
        try {
            return URI.create(localConfig.appiumUrl()).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Некорректный адрес Appium: " + localConfig.appiumUrl(), e);
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
