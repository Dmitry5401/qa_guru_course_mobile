package drivers;

import com.codeborne.selenide.WebDriverProvider;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static config.Project.auth;
import static config.Project.browserstack;

/**
 * Стенд {@code -DdeviceHost=browserstack}: Android-устройство в облаке App Automate.
 * Приложение туда загружается заранее, см. {@link config.BrowserstackConfig}.
 */
public class BrowserstackDriver implements WebDriverProvider, BrowserstackStand {

    @Nonnull
    @Override
    public WebDriver createDriver(@Nonnull Capabilities capabilities) {
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("userName", auth.user());
        browserstackOptions.put("accessKey", auth.accessKey());
        browserstackOptions.put("appiumVersion", browserstack.appiumVersion());
        browserstackOptions.put("deviceName", browserstack.androidDevice());
        browserstackOptions.put("osVersion", browserstack.androidOsVersion());
        browserstackOptions.put("projectName", "qa_guru_course_mobile");
        browserstackOptions.put("buildName", "android");
        browserstackOptions.put("sessionName", "android test");

        UiAutomator2Options options = new UiAutomator2Options();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(browserstack.androidApp());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new AndroidDriver(URI.create(browserstack.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Malformed hub URL: " + browserstack.hubUrl(), e);
        }
    }
}
