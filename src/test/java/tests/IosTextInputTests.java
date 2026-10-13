package tests;

import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;

/**
 * Тесты на iOS. Используется их собственное демо-приложение Sample iOS.
 */
public class IosTextInputTests extends TestBaseIos {

    private static final String TEXT = "selenide";

    @Test
    void enteredTextIsShownInOutputTest() {
        step("Переход на экран ввода текста", () -> {
            iosHomeScreen.openTextInput();
            iosTextInputScreen.checkReady();
        });

        step("Ввод текста", () -> iosTextInputScreen.enter(TEXT));

        step("Проверка, что введённый текст попал в вывод", () ->
                iosTextInputScreen.checkOutput(TEXT)
        );
    }
}
