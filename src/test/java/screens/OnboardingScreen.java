package screens;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.Wait;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.appium.java_client.AppiumBy.id;

/**
 * Первый запуск Wikipedia: четыре экрана знакомства с кнопкой Skip.
 * Показывается не всегда — на уже использованном устройстве приложение
 * открывается сразу на главном экране.
 */
public class OnboardingScreen {

    private final SelenideElement skipButton =
            $(id("org.wikipedia:id/fragment_onboarding_skip_button"));
    private final By skipButtonLocator = id("org.wikipedia:id/fragment_onboarding_skip_button");
    /**
     * Карточка поиска — признак главного экрана. Сам поиск живёт в {@link SearchScreen},
     * здесь идентификатор нужен только чтобы отличить «онбординг ещё на экране»
     * от «приложение уже готово к поиску».
     */
    private final By mainScreen = id("org.wikipedia:id/search_container");

    /**
     * Нажимает Skip, только если онбординг действительно на экране.
     * <p>
     * Онбординг живёт в данных приложения, а чистые данные к началу сессии — не гарантия,
     * а поведение по умолчанию: в облаке устройство каждый раз новое, а свой телефон
     * отдаёт приложение в том виде, в каком его оставили — уже пройденным онбордингом
     * вперёд. Безусловный {@code click()} в этом случае тридцать секунд искал кнопку
     * и падал с «Element not found», хотя приложение было на экране и готово к работе.
     * <p>
     * Поэтому ждём любой из двух стартовых экранов и нажимаем Skip только тогда, когда
     * он есть. Если не пришёл ни один, сообщение называет оба — это уже не про онбординг,
     * а про то, что на экране не приложение. Текст английский по той же причине, что и
     * у драйверов: на русской Windows кириллица в консоли нечитаема.
     */
    public void skipIfShown() {
        Wait().withMessage("neither the onboarding screen nor the Wikipedia main screen showed up")
                .until(driver -> !driver.findElements(skipButtonLocator).isEmpty()
                        || !driver.findElements(mainScreen).isEmpty());

        if (!getWebDriver().findElements(skipButtonLocator).isEmpty()) {
            skipButton.click();
        }
    }
}
