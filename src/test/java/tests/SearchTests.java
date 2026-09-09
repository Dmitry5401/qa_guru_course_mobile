package tests;

import com.codeborne.selenide.Configuration;
import drivers.BrowserstackMobileDriver;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;

public class SearchTests {

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
        open();
    }

    @AfterEach
    void closeApp() {
        closeWebDriver();
    }

    @Test
    void successfulSearchTest() {
        $(AppiumBy.accessibilityId("Search Wikipedia")).click();
        $(AppiumBy.id("org.wikipedia.alpha:id/search_src_text")).sendKeys("Appium");

        $$(AppiumBy.id("org.wikipedia.alpha:id/page_list_item_title"))
                .shouldHave(sizeGreaterThan(0));
    }
}
