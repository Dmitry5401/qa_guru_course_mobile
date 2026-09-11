package config;

import org.aeonbits.owner.ConfigFactory;

/**
 * Конфиги проекта, собранные Owner один раз на запуск.
 */
public final class Project {

    public static final AuthConfig auth = ConfigFactory.create(AuthConfig.class);
    public static final TestConfig testConfig = ConfigFactory.create(TestConfig.class);

    private Project() {
    }
}
