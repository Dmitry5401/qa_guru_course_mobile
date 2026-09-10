package drivers;

import com.codeborne.selenide.WebDriverProvider;
import config.BrowserstackConfig;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Создаёт для Selenide сессию iOS-приложения на BrowserStack App Automate.
 * Отличается от {@link BrowserstackMobileDriver} только набором опций: у iOS
 * автоматизация идёт через XCUITest, поэтому вместо {@code UiAutomator2Options}
 * используется {@code XCUITestOptions}, а устройство и версия ОС берутся из
 * iOS-настроек конфига.
 */
public class BrowserstackIosDriver implements WebDriverProvider {

    @Override
    public WebDriver createDriver(Capabilities capabilities) {
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("userName", BrowserstackConfig.user());
        browserstackOptions.put("accessKey", BrowserstackConfig.accessKey());
        browserstackOptions.put("appiumVersion", BrowserstackConfig.appiumVersion());
        browserstackOptions.put("deviceName", BrowserstackConfig.iosDevice());
        browserstackOptions.put("osVersion", BrowserstackConfig.iosOsVersion());
        browserstackOptions.put("projectName", "First Java Project");
        browserstackOptions.put("buildName", "browserstack-build-1");
        browserstackOptions.put("sessionName", "ios_sample_app_test");

        XCUITestOptions options = new XCUITestOptions();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(BrowserstackConfig.iosApp());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new IOSDriver(URI.create(BrowserstackConfig.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Некорректный адрес хаба: " + BrowserstackConfig.hubUrl(), e);
        }
    }
}
