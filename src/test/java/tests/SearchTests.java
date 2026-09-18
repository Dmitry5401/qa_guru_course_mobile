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
        // Приложение перед сессией ставится заново — и Appium так делает по умолчанию,
        // и BrowserStack выдаёт чистое устройство, — поэтому онбординг встречает
        // тест на каждом запуске, на любом стенде.
        step("Пропуск онбординга", () ->
            $(id("org.wikipedia:id/fragment_onboarding_skip_button")).click()
        );

        step("Ввод текста для поиска", () -> {
            // Ищем по resource-id, а не по content-desc «Search Wikipedia»: content-desc
            // переводится вместе с интерфейсом, и на устройстве с русской локалью
            // там «Поиск по Википедии». resource-id от языка не зависит.
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys("Appium");
        });

        step("Проверка результата поиска", () ->
            $$(id("org.wikipedia:id/page_list_item_title")).shouldHave(sizeGreaterThan(0))
        );
    }
}
