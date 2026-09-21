package tests;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static io.appium.java_client.AppiumBy.id;
import static io.qameta.allure.Allure.step;

public class SearchTests extends TestBase {

    @Test
    void successfulSearchTest() {
        step("Пропуск онбординга", TestBase::skipOnboarding);

        step("Ввод текста для поиска", () -> {
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys("Appium");
        });

        step("Проверка результата поиска", () ->
            $$(id("org.wikipedia:id/page_list_item_title")).shouldHave(sizeGreaterThan(0))
        );
    }
}
