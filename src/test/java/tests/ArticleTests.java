package tests;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static io.appium.java_client.AppiumBy.id;
import static io.qameta.allure.Allure.step;
import static org.openqa.selenium.By.xpath;

public class ArticleTests extends TestBase {

    @Test
    void openSelenideArticleTest() {
        step("Пропуск онбординга", () ->
            $(id("org.wikipedia:id/fragment_onboarding_skip_button")).click()
        );

        step("Поиск статьи о Selenide", () -> {
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys("Selenide");
        });

        step("Открытие статьи из результатов поиска", () ->
            $$(id("org.wikipedia:id/page_list_item_title"))
                .shouldHave(sizeGreaterThan(0))
                .findBy(exactText("Selenide"))
                .click()
        );

        step("Проверка, что открылся экран статьи", () -> {
            $(id("org.wikipedia:id/page_contents_container")).shouldBe(visible);

            // Своего resource-id у заголовка нет: тело статьи рисует WebView, и её
            // название попадает к нам как text самого WebView. Проверяем именно его —
            // так видно, что открылась нужная статья, а не просто какой-то экран.
            $(xpath("//android.webkit.WebView[@text='Selenide']")).shouldBe(visible);
        });
    }
}
