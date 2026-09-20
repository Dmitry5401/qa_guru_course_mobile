package tests;

import config.DeviceHost;
import config.DeviceHostConverter;
import drivers.BrowserstackDriver;
import drivers.EmulationDriver;
import drivers.RealDriver;
import org.junit.jupiter.api.Disabled;
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
 * <p>
 * Имена тестов и тексты assert'ов здесь английские: Gradle печатает их в консоль, а на
 * русской Windows (кодовая страница 866) кириллица оттуда выходит нечитаемой — ровно так
 * и получилось с прежними {@code @DisplayName}. Комментарии и шаги Allure остаются
 * русскими: их читают в IDE и в отчёте, там с UTF-8 проблем нет.
 */
@Disabled
class StandSelectionTest {

    @ParameterizedTest(name = "-DdeviceHost={0} -> {1}")
    @CsvSource({
        "browserstack, BROWSERSTACK",
        "emulation,    EMULATION",
        "real,         REAL",
        "REAL,         REAL",
        "  real  ,     REAL"
    })
    void deviceHostIsParsedIgnoringCase(String input, DeviceHost expected) {
        assertEquals(expected, new DeviceHostConverter().convert(null, input));
    }

    @Test
    void unknownDeviceHostListsAllowedValues() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new DeviceHostConverter().convert(null, "emulator"));

        assertTrue(thrown.getMessage().contains("browserstack | emulation | real"),
                "The message should list the allowed stands, not just report an error: "
                        + thrown.getMessage());
    }

    @Test
    void selectedStandIsFullyConfigured() {
        Class<?> driver = new TestBase().driver();

        switch (deviceHost()) {
            case BROWSERSTACK -> {
                assertEquals(BrowserstackDriver.class, driver);
                assertTrue(browserstack.hubUrl().startsWith("https://"), browserstack.hubUrl());
                assertFalse(browserstack.androidApp().isBlank(), "app is not set");
                assertFalse(auth.user().isBlank(), "BrowserStack user is not set");
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
