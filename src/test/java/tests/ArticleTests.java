package tests;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static io.appium.java_client.AppiumBy.accessibilityId;
import static io.appium.java_client.AppiumBy.id;
import static io.qameta.allure.Allure.step;
import static org.openqa.selenium.By.xpath;

public class ArticleTests extends TestBase {

    @Test
    void openSelenideArticleTest() {
        step("Поиск статьи о Selenide", () -> {
            $(accessibilityId("Search Wikipedia")).click();
            $(id("org.wikipedia.alpha:id/search_src_text")).sendKeys("Selenide");
        });

        step("Открытие статьи из результатов поиска", () ->
            $$(id("org.wikipedia.alpha:id/page_list_item_title"))
                .shouldHave(sizeGreaterThan(0))
                .findBy(exactText("Selenide"))
                .click()
        );

        step("Проверка, что открылся экран статьи", () -> {
            // Заголовок статьи живёт в тулбаре и своего resource-id не имеет
            $(xpath("//*[@resource-id='org.wikipedia.alpha:id/page_toolbar']"
                + "/android.widget.TextView"))
                .shouldHave(text("Selenide"));
            // Действия статьи есть только на её экране, но не в результатах поиска
            $(accessibilityId("Table of Contents")).shouldBe(visible);
            // Текст статьи не проверяем: приложение грузит его через Mobile Content Service,
            // который Wikimedia отключила (403, phabricator.wikimedia.org/T328036)
        });
    }
}
