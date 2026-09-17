package tests.local;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static io.appium.java_client.AppiumBy.id;
import static io.qameta.allure.Allure.step;

public class SearchTests extends TestBase {

    @Test
    void successfulSearchTest() {
        // Appium по умолчанию переустанавливает приложение перед сессией,
        // поэтому онбординг встречает нас на каждом запуске.
        step("Skip onboarding", () ->
                $(id("org.wikipedia:id/fragment_onboarding_skip_button")).click());

        step("Type search", () -> {
            // Строку поиска ищем по resource-id, а не по content-desc «Search Wikipedia»:
            // content-desc переводится вместе с интерфейсом, и на телефоне с русской
            // локалью там «Поиск по Википедии». Локальный прогон идёт на личном
            // устройстве с любым языком, resource-id от языка не зависит.
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys("Appium");
        });

        step("Verify content found", () ->
                $$(id("org.wikipedia:id/page_list_item_title")).shouldHave(sizeGreaterThan(0)));
    }
}
