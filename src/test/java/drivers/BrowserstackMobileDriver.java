package drivers;

import com.codeborne.selenide.WebDriverProvider;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static config.Project.auth;
import static config.Project.testConfig;

/**
 * Создаёт для Selenide сессию мобильного приложения на BrowserStack App Automate.
 * Подключается через {@code Configuration.browser = BrowserstackMobileDriver.class.getName()}.
 */
public class BrowserstackMobileDriver implements WebDriverProvider {

    @Override
    public WebDriver createDriver(Capabilities capabilities) {
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("userName", auth.user());
        browserstackOptions.put("accessKey", auth.accessKey());
        browserstackOptions.put("appiumVersion", testConfig.appiumVersion());
        browserstackOptions.put("deviceName", testConfig.androidDevice());
        browserstackOptions.put("osVersion", testConfig.androidOsVersion());
        browserstackOptions.put("projectName", "First Java Project");
        browserstackOptions.put("buildName", "browserstack-build-1");
        browserstackOptions.put("sessionName", "first_test");

        UiAutomator2Options options = new UiAutomator2Options();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(testConfig.app());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new AndroidDriver(URI.create(testConfig.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Некорректный адрес хаба: " + testConfig.hubUrl(), e);
        }
    }
}
