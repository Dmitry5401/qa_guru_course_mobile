package screens;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static io.appium.java_client.AppiumBy.accessibilityId;

/**
 * Экран ввода текста в демо-приложении Sample iOS.
 * Перевод строки закрывает клавиатуру и отправляет текст в вывод.
 */
public class IosTextInputScreen {

    private static final String EMPTY_OUTPUT = "Waiting for text input.";

    private final SelenideElement input = $(accessibilityId("Text Input"));
    private final SelenideElement output = $(accessibilityId("Text Output"));

    public void checkReady() {
        input.shouldBe(visible);
        output.shouldHave(exactText(EMPTY_OUTPUT));
    }

    public void enter(String text) {
        input.sendKeys(text + "\n");
    }

    public void checkOutput(String text) {
        output.shouldHave(exactText(text));
    }
}
