package tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverProvider;
import com.codeborne.selenide.logevents.SelenideLogger;
import drivers.BrowserstackDriver;
import drivers.BrowserstackStand;
import drivers.EmulationDriver;
import drivers.RealDriver;
import helpers.Attach;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static config.Project.deviceHost;

public class TestBase {

    @BeforeAll
    static void configureSelenide() {
        // размер окна и таймаут загрузки страницы в нативном приложении не поддерживаются
        Configuration.browserSize = null;
        Configuration.pageLoadTimeout = -1;
        Configuration.timeout = 30000;
    }

    /**
     * Драйвер под выбранный стенд. Android-тесты берут его из {@code -DdeviceHost},
     * iOS-тесты переопределяют метод: локального стенда для iOS в проекте нет.
     * <p>
     * Selenide читает {@code Configuration.browser} в момент {@code open()}, поэтому
     * драйвер можно выбирать для каждого класса тестов отдельно.
     */
    protected Class<? extends WebDriverProvider> driver() {
        return switch (deviceHost()) {
            case BROWSERSTACK -> BrowserstackDriver.class;
            case EMULATION -> EmulationDriver.class;
            case REAL -> RealDriver.class;
        };
    }

    @BeforeEach
    void startApp() {
        Configuration.browser = driver().getName();
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        open();
    }

    @AfterEach
    void closeApp() {
        // Скриншот и вёрстку снимаем до закрытия сессии: после закрытия драйвер уже недоступен
        Attach.screenshotAs("Last screenshot");
        Attach.pageSource();

        // Видео пишет только BrowserStack, и ссылка на него живёт в его же API,
        // поэтому для локальных стендов шага нет — там нечего прикладывать.
        boolean videoAvailable = BrowserstackStand.class.isAssignableFrom(driver());
        String sessionId = videoAvailable ? Selenide.sessionId().toString() : null;

        closeWebDriver();

        if (videoAvailable) {
            Attach.addVideo(sessionId);
        }
    }
}
