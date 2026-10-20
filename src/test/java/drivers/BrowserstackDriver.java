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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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
        browserstackOptions.put("osVersion", resolveAndroidOsVersion(
                browserstack.androidDevice(), browserstack.androidOsVersion()));
        browserstackOptions.put("projectName", "qa_guru_course_mobile");
        browserstackOptions.put("buildName", "android");
        browserstackOptions.put("sessionName", "android test");

        UiAutomator2Options options = new UiAutomator2Options();
        options.merge(capabilities);
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        options.setApp(browserstack.androidApp());
        // Язык устройства задаём явно: от него зависит язык статей в приложении, а
        // ArticleTests сверяет их содержимое. См. BrowserstackConfig#androidLanguage.
        options.setLanguage(browserstack.androidLanguage());
        options.setLocale(browserstack.androidLocale());
        options.setCapability("bstack:options", browserstackOptions);

        try {
            return new AndroidDriver(URI.create(browserstack.hubUrl()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Malformed hub URL: " + browserstack.hubUrl(), e);
        }
    }

    /**
     * Линейка Samsung Galaxy S22 (S22, S22 Plus, S22 Ultra) в App Automate
     * осталась только с Android 12.0. Версия 13.0 в {@code browserstack.properties}
     * — для Galaxy S23. Джоба Jenkins передаёт S22 вместе с 13.0, и BrowserStack
     * отвечает {@code BROWSERSTACK_INVALID_OS_VERSION}. Для этой линейки
     * подставляется единственная живая версия.
     */
    static String resolveAndroidOsVersion(String device, String osVersion) {
        String normalizedDevice = device == null ? "" : device.toLowerCase(Locale.ROOT).trim();
        String normalizedOs = osVersion == null ? "" : osVersion.trim();
        if (GALAXY_S22.matcher(normalizedDevice).find() && "13.0".equals(normalizedOs)) {
            return "12.0";
        }
        return osVersion;
    }

    /** Граница слова, чтобы S23 и посторонние имена не попали под S22. */
    private static final Pattern GALAXY_S22 = Pattern.compile("\\bs22\\b");
}
