package config;

import org.aeonbits.owner.ConfigFactory;

/**
 * Конфиги проекта, собранные Owner один раз на запуск.
 * <p>
 * Конфиги всех стендов создаются сразу, а не только выбранный: чтение свойств —
 * это разбор пары небольших файлов, зато обращение к конфигу не зависит от порядка
 * инициализации. Отсутствующий файл Owner просто пропускает.
 */
public final class Project {

    public static final ProjectConfig projectConfig = ConfigFactory.create(ProjectConfig.class);
    public static final AuthConfig auth = ConfigFactory.create(AuthConfig.class);

    public static final BrowserstackConfig browserstack =
            ConfigFactory.create(BrowserstackConfig.class);
    public static final EmulationConfig emulation = ConfigFactory.create(EmulationConfig.class);
    public static final RealConfig real = ConfigFactory.create(RealConfig.class);

    /** Стенд из {@code -DdeviceHost}; без ключа — {@link DeviceHost#BROWSERSTACK}. */
    public static DeviceHost deviceHost() {
        return projectConfig.deviceHost();
    }

    private Project() {
    }
}
