package config;

import org.aeonbits.owner.Config;

/**
 * Приложение сначала нужно загрузить в App Automate и получить app_url:
 * <pre>
 * curl -u "USER:ACCESS_KEY" \
 *   -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
 *   -F "file=@/path/to/app.apk" -F 'data={"custom_id": "WikipediaStable27"}'
 * </pre>
 * В качестве приложения годится и {@code app_url} вида {@code bs://<hash>}, и custom_id.
 * custom_id удобнее: он переживает повторную загрузку, а app_url каждый раз новый.
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
