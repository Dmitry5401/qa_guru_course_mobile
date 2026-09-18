package tests;

import config.DeviceHost;
import config.DeviceHostConverter;
import drivers.BrowserstackDriver;
import drivers.EmulationDriver;
import drivers.RealDriver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static config.Project.auth;
import static config.Project.browserstack;
import static config.Project.deviceHost;
import static config.Project.emulation;
import static config.Project.real;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет разбор ключа {@code -DdeviceHost} и то, что выбранный стенд собран целиком:
 * драйвер тот, что ожидается, и его конфиг заполнен.
 * <p>
 * Устройство для этого не нужно — сессия не поднимается, — поэтому тест идёт на любой
 * машине и в CI. Стенд в него приходит из того же ключа, что и в остальные тесты,
 * так что три прогона с разными значениями покрывают все три стенда.
 */
class StandSelectionTest {

    @ParameterizedTest(name = "-DdeviceHost={0} -> {1}")
    @CsvSource({
        "browserstack, BROWSERSTACK",
        "emulation,    EMULATION",
        "real,         REAL",
        "REAL,         REAL",
        "  real  ,     REAL"
    })
    @DisplayName("Значение deviceHost разбирается без оглядки на регистр и пробелы")
    void deviceHostIsParsedIgnoringCase(String input, DeviceHost expected) {
        assertEquals(expected, new DeviceHostConverter().convert(null, input));
    }

    @Test
    @DisplayName("Незнакомый стенд подсказывает список допустимых значений")
    void unknownDeviceHostListsAllowedValues() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new DeviceHostConverter().convert(null, "emulator"));

        assertTrue(thrown.getMessage().contains("browserstack | emulation | real"),
                "Сообщение должно называть допустимые стенды, а не только ошибку: "
                        + thrown.getMessage());
    }

    @Test
    @DisplayName("Выбранный стенд даёт свой драйвер и заполненный конфиг")
    void selectedStandIsFullyConfigured() {
        Class<?> driver = new TestBase().driver();

        switch (deviceHost()) {
            case BROWSERSTACK -> {
                assertEquals(BrowserstackDriver.class, driver);
                assertTrue(browserstack.hubUrl().startsWith("https://"), browserstack.hubUrl());
                assertFalse(browserstack.androidApp().isBlank(), "не задано приложение");
                assertFalse(auth.user().isBlank(), "не задан логин BrowserStack");
            }
            case EMULATION -> {
                assertEquals(EmulationDriver.class, driver);
                assertLocalStandIsConfigured(emulation.appiumUrl(), emulation.appPackage());
            }
            case REAL -> {
                assertEquals(RealDriver.class, driver);
                assertLocalStandIsConfigured(real.appiumUrl(), real.appPackage());
            }
        }
    }

    /**
     * Адрес сервера и приложение локальные стенды берут из общего local.properties
     * через наследование в {@code LocalStandConfig}, поэтому проверка у них одна.
     */
    private void assertLocalStandIsConfigured(String appiumUrl, String appPackage) {
        assertTrue(appiumUrl.startsWith("http://"), appiumUrl);
        assertEquals("org.wikipedia", appPackage);
    }
}
