package drivers;

import com.codeborne.selenide.WebDriverProvider;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static config.Project.auth;
import static config.Project.testConfig;

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
        browserstackOptions.put("userName", auth.user());
        browserstackOptions.put("accessKey", auth.accessKey());
        browserstackOptions.put("appiumVersion", testConfig.appiumVersion());
        browserstackOptions.put("deviceName", testConfig.iosDevice());
        browserstackOptions.put("osVersion", testConfig.iosOsVersion());
        browserstackOptions.put("projectName", "First Java Project");
        browserstackOptions.put("buildName", "browserstack-build-1");
        browserstackOptions.put("sessionName", "ios_sample_app_test");

        XCUITestOptions options = new XCUITestOptions();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(testConfig.iosApp());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new IOSDriver(URI.create(testConfig.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Некорректный адрес хаба: " + testConfig.hubUrl(), e);
        }
    }
}
