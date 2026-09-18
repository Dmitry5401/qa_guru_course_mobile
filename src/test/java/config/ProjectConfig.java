package config;

import org.aeonbits.owner.Config;

/**
 * Настройки самого прогона, не привязанные к стенду.
 * <p>
 * Источники — только системные свойства и переменные окружения: стенд выбирают
 * в командной строке или в задании CI, файла для него нет.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env"
})
public interface ProjectConfig extends Config {

    @Key("deviceHost")
    @DefaultValue("browserstack")
    @ConverterClass(DeviceHostConverter.class)
    DeviceHost deviceHost();
}
