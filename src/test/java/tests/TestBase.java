package tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
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
        Configuration.browser = BrowserstackMobileDriver.class.getName();
        // размер окна и таймаут загрузки страницы в нативном приложении не поддерживаются
        Configuration.browserSize = null;
        Configuration.pageLoadTimeout = -1;
        Configuration.timeout = 30000;
    }

    @BeforeEach
    void startApp() {
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        open();
    }

    @AfterEach
    void closeApp() {
        String sessionId = Selenide.sessionId().toString();
        System.out.println(sessionId);
        //Attach.screenshotAs("Last screenshot"); // todo fix
        Attach.pageSource();
        closeWebDriver();
        Attach.addVideo(sessionId);
    }
}
