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

    /**
     * Что ищем и что ожидаем увидеть заголовком. Одна константа на весь тест: когда
     * название было вписано в три места руками, правка «Sony» на «Java» применилась не
     * везде, и тест падал на сверке заголовка со старым значением.
     */
    private static final String ARTICLE = "Java";

    @Test
    void openJavaArticleTest() {
        step("Пропуск онбординга", () ->
            $(id("org.wikipedia:id/fragment_onboarding_skip_button")).click()
        );

        step("Поиск статьи «" + ARTICLE + "»", () -> {
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys(ARTICLE);
        });

        step("Открытие статьи из результатов поиска", () ->
            // Именно exactText: по «Java» выдача возвращает ещё JavaScript, JavaFX и
            // JavaServer Pages, и вхождение подстроки открыло бы первую попавшуюся из них.
            // Тогда сверять заголовок было бы не с чем — статья открылась бы не та.
            $$(id("org.wikipedia:id/page_list_item_title"))
                .shouldHave(sizeGreaterThan(0))
                .findBy(exactText(ARTICLE))
                .click()
        );

        step("Проверка, что открылась выбранная статья", () -> {
            $(id("org.wikipedia:id/page_contents_container")).shouldBe(visible);

            // Своего resource-id у заголовка нет: тело статьи рисует WebView. Название
            // приходит к нам либо как text самого WebView, либо как text вложенного
            // TextView — что именно, зависит от сборки Android System WebView на
            // устройстве. Поэтому спрашиваем любой узел внутри контейнера статьи, а не
            // прибиваем локатор к android.webkit.WebView, как было раньше.
            $(xpath("//*[@resource-id='org.wikipedia:id/page_contents_container']"
                    + "//*[@text='" + ARTICLE + "']")).shouldBe(visible);
        });
    }
}
