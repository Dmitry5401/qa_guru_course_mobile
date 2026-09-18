package config;

import org.aeonbits.owner.Config;

/**
 * Стенд {@code -DdeviceHost=emulation}: эмулятор на своей машине.
 * Общие для локальных стендов ключи — в {@link LocalStandConfig}.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:emulation.properties",
    "classpath:local.properties"
})
public interface EmulationConfig extends LocalStandConfig {

    /**
     * Имя виртуального устройства из Device Manager, например {@code Pixel_10a}.
     * Для эмулятора это удобнее серийника: имя не меняется, а номер в
     * {@code emulator-5554} зависит от порядка запуска. Appium по этому ключу ещё
     * и сам поднимет эмулятор, если тот не запущен.
     * <p>
     * Пусто — возьмётся первое устройство из {@code adb devices}.
     */
    @Key("EMULATION_AVD")
    @DefaultValue("")
    String avd();
}
