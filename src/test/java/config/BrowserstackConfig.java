package config;

import org.aeonbits.owner.Config;

/**
 * Стенд {@code -DdeviceHost=browserstack}: устройство в облаке App Automate.
 * Логин и ключ лежат отдельно, в {@link AuthConfig}.
 * <p>
 * Приложение сначала нужно загрузить в App Automate и получить app_url:
 * <pre>
 * curl -u "USER:ACCESS_KEY" \
 *   -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
 *   -F "file=@/path/to/app.apk" -F 'data={"custom_id": "WikipediaStable27"}'
 * </pre>
 * В качестве приложения годится и {@code app_url} вида {@code bs://<hash>}, и custom_id.
 * custom_id удобнее: он переживает повторную загрузку, а app_url каждый раз новый.
 * <p>
 * Настройки iOS живут здесь же: локального стенда для iOS в проекте нет, для него
 * нужен macOS с Xcode, поэтому iOS-тесты всегда идут в облако.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:browserstack.properties"
})
public interface BrowserstackConfig extends Config {

    @Key("BROWSERSTACK_HUB")
    String hubUrl();

    @Key("BROWSERSTACK_APPIUM_VERSION")
    String appiumVersion();

    @Key("BROWSERSTACK_ANDROID_APP_ID")
    String androidApp();

    @Key("BROWSERSTACK_ANDROID_DEVICE")
    String androidDevice();

    @Key("BROWSERSTACK_ANDROID_OS_VERSION")
    String androidOsVersion();

    /**
     * Язык и регион, в которые BrowserStack переведёт устройство перед сессией.
     * Нужны из-за {@code ArticleTests}: Википедия выбирает язык статей по языку
     * устройства, а статьи «Java» в русском и английском разделах — про разное, так что
     * без этих ключей один и тот же тест видел бы в облаке не то, что на своём телефоне.
     * Подробнее — в {@code docs/stands.md}.
     */
    @Key("BROWSERSTACK_ANDROID_LANGUAGE")
    String androidLanguage();

    @Key("BROWSERSTACK_ANDROID_LOCALE")
    String androidLocale();

    @Key("BROWSERSTACK_IOS_APP_ID")
    String iosApp();

    @Key("BROWSERSTACK_IOS_DEVICE")
    String iosDevice();

    @Key("BROWSERSTACK_IOS_OS_VERSION")
    String iosOsVersion();
}
