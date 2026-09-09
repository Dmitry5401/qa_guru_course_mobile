package drivers;

import com.codeborne.selenide.WebDriverProvider;
import config.BrowserstackConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Создаёт для Selenide сессию мобильного приложения на BrowserStack App Automate.
 * Подключается через {@code Configuration.browser = BrowserstackMobileDriver.class.getName()}.
 */
public class BrowserstackMobileDriver implements WebDriverProvider {

    @Override
    public WebDriver createDriver(Capabilities capabilities) {
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("userName", BrowserstackConfig.user());
        browserstackOptions.put("accessKey", BrowserstackConfig.accessKey());
        browserstackOptions.put("appiumVersion", BrowserstackConfig.appiumVersion());
        browserstackOptions.put("deviceName", BrowserstackConfig.device());
        browserstackOptions.put("osVersion", BrowserstackConfig.osVersion());
        browserstackOptions.put("projectName", "First Java Project");
        browserstackOptions.put("buildName", "browserstack-build-1");
        browserstackOptions.put("sessionName", "first_test");

        UiAutomator2Options options = new UiAutomator2Options();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(BrowserstackConfig.app());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new AndroidDriver(URI.create(BrowserstackConfig.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Некорректный адрес хаба: " + BrowserstackConfig.hubUrl(), e);
        }
    }
}
