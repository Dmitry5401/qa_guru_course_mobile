package config;

import org.aeonbits.owner.Config;

/**
 * Настройки локального прогона на эмуляторе или подключённом по USB устройстве.
 * Про {@code LoadType.MERGE} и имена ключей — см. {@link AuthConfig}.
 * <p>
 * {@code LOCAL_APPIUM_URL} должен совпадать с базовым путём сервера, иначе тот отдаёт 404.
 * Здесь стоит {@code /wd/hub} — под сервер, поднятый как {@code appium --base-path /wd/hub}.
 * Если запускать сервер просто командой {@code appium}, он слушает корень, и суффикс надо
 * убрать: у Appium 2 и 3 базовый путь по умолчанию корневой, {@code /wd/hub} остался
 * в Appium 1. Расхождение ловит проверка в {@link drivers.LocalDriver} до создания сессии.
 * <p>
 * Устройство Appium выбирает по {@code udid}: если он не задан, в дело идёт
 * {@code platformVersion}, а если и его нет — первое устройство из {@code adb devices}.
 * Когда эмулятор и телефон подключены одновременно, udid — единственный надёжный способ
 * попасть в нужное; список серийников показывает {@code adb devices}.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local.properties"
})
public interface LocalConfig extends Config {

    @Key("LOCAL_APPIUM_URL")
    @DefaultValue("http://localhost:4723/wd/hub")
    String appiumUrl();

    @Key("LOCAL_DEVICE_UDID")
    @DefaultValue("")
    String deviceUdid();

    @Key("LOCAL_PLATFORM_VERSION")
    @DefaultValue("")
    String platformVersion();

    @Key("LOCAL_APP_PACKAGE")
    String appPackage();

    @Key("LOCAL_APP_ACTIVITY")
    String appActivity();

    @Key("LOCAL_APP_URL")
    String appUrl();
}
