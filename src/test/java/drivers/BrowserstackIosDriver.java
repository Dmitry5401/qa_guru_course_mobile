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
import static config.Project.browserstack;

/**
 * Создаёт для Selenide сессию iOS-приложения на BrowserStack App Automate.
 * Отличается от {@link BrowserstackDriver} только набором опций: у iOS
 * автоматизация идёт через XCUITest, поэтому вместо {@code UiAutomator2Options}
 * используется {@code XCUITestOptions}, а устройство и версия ОС берутся из
 * iOS-настроек конфига.
 */
public class BrowserstackIosDriver implements WebDriverProvider, BrowserstackStand {

    @Override
    public WebDriver createDriver(Capabilities capabilities) {
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("userName", auth.user());
        browserstackOptions.put("accessKey", auth.accessKey());
        browserstackOptions.put("appiumVersion", browserstack.appiumVersion());
        browserstackOptions.put("deviceName", browserstack.iosDevice());
        browserstackOptions.put("osVersion", browserstack.iosOsVersion());
        browserstackOptions.put("projectName", "qa_guru_course_mobile");
        browserstackOptions.put("buildName", "ios");
        browserstackOptions.put("sessionName", "ios test");

        XCUITestOptions options = new XCUITestOptions();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(browserstack.iosApp());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new IOSDriver(URI.create(browserstack.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Malformed hub URL: " + browserstack.hubUrl(), e);
        }
    }
}
