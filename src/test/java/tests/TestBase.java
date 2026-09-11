package tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverProvider;
import com.codeborne.selenide.logevents.SelenideLogger;
import drivers.BrowserstackMobileDriver;
import helpers.Attach;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;

public class TestBase {
    @BeforeAll
    static void configureSelenide() {
        // размер окна и таймаут загрузки страницы в нативном приложении не поддерживаются
        Configuration.browserSize = null;
        Configuration.pageLoadTimeout = -1;
        Configuration.timeout = 30000;
    }

    /**
     * Драйвер по умолчанию — Android. iOS-тесты переопределяют этот метод.
     * Selenide читает {@code Configuration.browser} в момент {@code open()},
     * поэтому платформу можно выбирать для каждого класса тестов отдельно.
     */
    protected Class<? extends WebDriverProvider> driver() {
        return BrowserstackMobileDriver.class;
    }

    @BeforeEach
    void startApp() {
        Configuration.browser = driver().getName();
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        open();
    }

    @AfterEach
    void closeApp() {
        String sessionId = Selenide.sessionId().toString();
        System.out.println(sessionId);
        Attach.screenshotAs("Last screenshot");
        Attach.pageSource();
        closeWebDriver();
        Attach.addVideo(sessionId);
    }

}
