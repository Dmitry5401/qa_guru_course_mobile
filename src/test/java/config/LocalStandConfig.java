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

    /**
     * Язык и регион, в которые Appium переведёт устройство перед сессией. Нужны из-за
     * {@code ArticleTests}: Википедия выбирает язык статей по языку устройства, а статьи
     * «Java» в русском и английском разделах — про разное.
     * <p>
     * Пусто — язык устройства остаётся как есть. Так и стоит у стенда {@code real}:
     * менять язык личного телефона ради теста не дело, а Appium обратно его не вернёт.
     * Если телефон не русский, язык можно задать на один прогон:
     * {@code -DLOCAL_LANGUAGE=ru -DLOCAL_LOCALE=RU}.
     */
    @Key("LOCAL_LANGUAGE")
    @DefaultValue("")
    String language();

    @Key("LOCAL_LOCALE")
    @DefaultValue("")
    String locale();
}
