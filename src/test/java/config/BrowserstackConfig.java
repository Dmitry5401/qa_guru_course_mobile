package config;

/**
 * Настройки запуска на BrowserStack App Automate.
 * <p>
 * Значения читаются из системных свойств Gradle (-Dbrowserstack.app=...) или,
 * если свойство не задано, из переменных окружения. Секреты и app_url не хранятся в коде.
 * <p>
 * Приложение сначала нужно загрузить в App Automate и получить app_url:
 * <pre>
 * curl -u "USER:ACCESS_KEY" \
 *   -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
 *   -F "file=@/path/to/app.apk" -F 'data={"custom_id": "WikipediaSample"}'
 * </pre>
 * Ответ содержит {@code app_url} вида {@code bs://<hash>}; его (или custom_id)
 * и нужно передать в {@code browserstack.app}.
 */
public final class BrowserstackConfig {

    private BrowserstackConfig() {
    }

    public static String user() {
        return required("browserstack.user", "BROWSERSTACK_USERNAME");
    }

    public static String accessKey() {
        return required("browserstack.key", "BROWSERSTACK_ACCESS_KEY");
    }

    public static String app() {
        return required("browserstack.app", "BROWSERSTACK_APP_ID");
    }

    public static String device() {
        return optional("browserstack.device", "BROWSERSTACK_DEVICE", "Samsung Galaxy S22 Ultra");
    }

    public static String osVersion() {
        return optional("browserstack.osVersion", "BROWSERSTACK_OS_VERSION", "12.0");
    }

    public static String appiumVersion() {
        return optional("browserstack.appiumVersion", "BROWSERSTACK_APPIUM_VERSION", "2.0.1");
    }

    public static String hubUrl() {
        return optional("browserstack.hub", "BROWSERSTACK_HUB", "https://hub.browserstack.com/wd/hub");
    }

    private static String required(String systemProperty, String environmentVariable) {
        String value = optional(systemProperty, environmentVariable, null);
        if (value == null) {
            throw new IllegalStateException(
                    "Не задано обязательное значение: передайте -D" + systemProperty
                            + "=... или переменную окружения " + environmentVariable);
        }
        return value;
    }

    private static String optional(String systemProperty, String environmentVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (isBlank(value)) {
            value = System.getenv(environmentVariable);
        }
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
