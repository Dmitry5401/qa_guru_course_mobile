package config;

/**
 * Настройки запуска на BrowserStack App Automate.
 * <p>
 * Значения по умолчанию хранятся здесь строками, поэтому тесты запускаются без
 * дополнительной настройки. Любое из них можно переопределить системным свойством
 * (например, {@code -Dbrowserstack.app=bs://...}) или переменной окружения.
 * <p>
 * Приложение сначала нужно загрузить в App Automate и получить app_url:
 * <pre>
 * curl -u "USER:ACCESS_KEY" \
 *   -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
 *   -F "file=@/path/to/app.apk" -F 'data={"custom_id": "WikipediaSample"}'
 * </pre>
 * Ответ содержит {@code app_url} вида {@code bs://<hash>}; в {@code APP} можно
 * положить как его, так и custom_id приложения.
 */
public final class BrowserstackConfig {

    private static final String USER = "dmitry_8rmWIH";
    private static final String ACCESS_KEY = "zW2u3gAFLNoZwF4qi874";
    private static final String APP = "WikipediaSample";
    private static final String DEVICE = "Samsung Galaxy S22 Ultra";
    private static final String OS_VERSION = "12.0";
    private static final String APPIUM_VERSION = "2.0.1";
    private static final String HUB_URL = "https://hub.browserstack.com/wd/hub";

    private BrowserstackConfig() {
    }

    public static String user() {
        return value("browserstack.user", "BROWSERSTACK_USERNAME", USER);
    }

    public static String accessKey() {
        return value("browserstack.key", "BROWSERSTACK_ACCESS_KEY", ACCESS_KEY);
    }

    public static String app() {
        return value("browserstack.app", "BROWSERSTACK_APP_ID", APP);
    }

    public static String device() {
        return value("browserstack.device", "BROWSERSTACK_DEVICE", DEVICE);
    }

    public static String osVersion() {
        return value("browserstack.osVersion", "BROWSERSTACK_OS_VERSION", OS_VERSION);
    }

    public static String appiumVersion() {
        return value("browserstack.appiumVersion", "BROWSERSTACK_APPIUM_VERSION", APPIUM_VERSION);
    }

    public static String hubUrl() {
        return value("browserstack.hub", "BROWSERSTACK_HUB", HUB_URL);
    }

    private static String value(String systemProperty, String environmentVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (isBlank(value)) {
            value = System.getenv(environmentVariable);
        }
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String getUser(){return USER;}

    public static String getAccessKey(){return ACCESS_KEY;}
}
