package config;

import org.aeonbits.owner.Config;

/**
 * Стенд {@code -DdeviceHost=real}: телефон, подключённый к своей машине по USB.
 * Общие для локальных стендов ключи — в {@link LocalStandConfig}.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:real.properties",
    "classpath:local.properties"
})
public interface RealConfig extends LocalStandConfig {

    /**
     * Серийник устройства из {@code adb devices}, например {@code R58R611HJWD}.
     * Appium смотрит именно на него: {@code deviceName} на выбор устройства не влияет.
     * <p>
     * Пусто — возьмётся первое устройство из списка, что рискованно, когда рядом
     * поднят эмулятор: он может оказаться первым.
     */
    @Key("REAL_DEVICE_UDID")
    @DefaultValue("")
    String deviceUdid();
}
