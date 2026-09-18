package config;

import org.aeonbits.owner.Config;

/**
 * Общее для стендов, которые работают через Appium на своей машине, —
 * {@link EmulationConfig} и {@link RealConfig}. Оба ходят на один и тот же сервер
 * и ставят одно и то же приложение, различаются только выбором устройства.
 * <p>
 * Значения лежат в {@code local.properties}. Наследники подключают этот файл
 * вторым источником, после своего: Owner с {@code LoadType.MERGE} читает источники
 * по порядку, поэтому стенд при необходимости может перебить любой общий ключ.
 */
public interface LocalStandConfig extends Config {

    @Key("LOCAL_APPIUM_URL")
    @DefaultValue("http://localhost:4723/wd/hub")
    String appiumUrl();

    @Key("LOCAL_APP_URL")
    String appUrl();

    @Key("LOCAL_APP_PACKAGE")
    String appPackage();

    @Key("LOCAL_APP_ACTIVITY")
    String appActivity();
}
