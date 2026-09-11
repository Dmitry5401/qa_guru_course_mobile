package config;

import org.aeonbits.owner.Config;

/**
 * Настройки прогона: адрес хаба, приложения и устройства.
 * Про {@code LoadType.MERGE} и имена ключей — см. {@link AuthConfig}.
 * <p>
 * Приложение сначала нужно загрузить в App Automate и получить app_url:
 * <pre>
 * curl -u "USER:ACCESS_KEY" \
 *   -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
 *   -F "file=@/path/to/app.apk" -F 'data={"custom_id": "WikipediaSample"}'
 * </pre>
 * В качестве приложения годится и {@code app_url} вида {@code bs://<hash>}, и custom_id.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:test.properties"
})
public interface TestConfig extends Config {

    @Key("BROWSERSTACK_HUB")
    String hubUrl();

    @Key("BROWSERSTACK_APPIUM_VERSION")
    String appiumVersion();

    @Key("BROWSERSTACK_APP_ID")
    String app();

    @Key("BROWSERSTACK_DEVICE")
    String device();

    @Key("BROWSERSTACK_OS_VERSION")
    String osVersion();

    @Key("BROWSERSTACK_IOS_APP_ID")
    String iosApp();

    @Key("BROWSERSTACK_IOS_DEVICE")
    String iosDevice();

    @Key("BROWSERSTACK_IOS_OS_VERSION")
    String iosOsVersion();
}
