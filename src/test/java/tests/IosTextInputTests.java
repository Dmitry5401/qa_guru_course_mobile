package tests;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static io.appium.java_client.AppiumBy.accessibilityId;
import static io.qameta.allure.Allure.step;

/**
 * Тесты на iOS. Приложения Wikipedia для iOS у BrowserStack нет, поэтому здесь
 * используется их собственное демо-приложение Sample iOS.
 */
public class IosTextInputTests extends TestBaseIos {

    private static final String TEXT = "selenide@qa.guru";

    @Test
    void enteredTextIsShownInOutputTest() {
        step("Переход на экран ввода текста", () -> {
            $(accessibilityId("Text Button")).click();
            $(accessibilityId("Text Input")).shouldBe(visible);
            $(accessibilityId("Text Output")).shouldHave(exactText("Waiting for text input."));
        });

        // перевод строки закрывает клавиатуру и отправляет текст в вывод
        step("Ввод текста", () -> $(accessibilityId("Text Input")).sendKeys(TEXT + "\n"));

        step("Проверка, что введённый текст попал в вывод", () ->
            $(accessibilityId("Text Output")).shouldHave(exactText(TEXT))
        );
    }
}
