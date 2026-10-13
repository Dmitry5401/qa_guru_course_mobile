package tests;

import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;

public class SearchTests extends TestBase {

    private static final String QUERY = "Appium";

    @Test
    void successfulSearchTest() {
        step("Пропуск онбординга", onboardingScreen::skipIfShown);

        step("Ввод текста для поиска", () -> searchScreen.search(QUERY));

        step("Проверка результата поиска", searchScreen::checkResultsNotEmpty);
    }
}
