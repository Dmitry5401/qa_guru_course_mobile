package tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverProvider;
import com.codeborne.selenide.logevents.SelenideLogger;
import drivers.BrowserstackDriver;
import drivers.BrowserstackStand;
import drivers.EmulationDriver;
import drivers.RealDriver;
import helpers.Attach;
import helpers.SessionDiagnostics;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.Wait;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static config.Project.deviceHost;
import static io.appium.java_client.AppiumBy.id;

@ExtendWith(SessionDiagnostics.class)
public class TestBase {

    @BeforeAll
    static void configureSelenide() {
        // размер окна и таймаут загрузки страницы в нативном приложении не поддерживаются
        Configuration.browserSize = null;
        Configuration.pageLoadTimeout = -1;
        Configuration.timeout = 30000;
    }

    /**
     * Драйвер под выбранный стенд. Android-тесты берут его из {@code -DdeviceHost},
     * iOS-тесты переопределяют метод: локального стенда для iOS в проекте нет.
     * <p>
     * Selenide читает {@code Configuration.browser} в момент {@code open()}, поэтому
     * драйвер можно выбирать для каждого класса тестов отдельно.
     */
    protected Class<? extends WebDriverProvider> driver() {
        return switch (deviceHost()) {
            case BROWSERSTACK -> BrowserstackDriver.class;
            case EMULATION -> EmulationDriver.class;
            case REAL -> RealDriver.class;
        };
    }

    @BeforeEach
    void startApp() {
        Configuration.browser = driver().getName();
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        open();
    }

    /**
     * Пропускает онбординг, если приложение его показывает.
     * <p>
     * Показывает не всегда. Онбординг живёт в данных приложения, а чистые данные к началу
     * сессии — не гарантия, а поведение по умолчанию: в облаке устройство каждый раз
     * новое, а свой телефон отдаёт приложение в том виде, в каком его оставили — уже
     * пройденным онбордингом вперёд. Безусловный {@code click()} в этом случае тридцать
     * секунд искал кнопку и падал с «Element not found», хотя приложение было на экране
     * и готово к работе.
     * <p>
     * Поэтому ждём любой из двух стартовых экранов и нажимаем «Skip» только тогда, когда
     * он есть. Если не пришёл ни один, сообщение называет оба — это уже не про онбординг,
     * а про то, что на экране не приложение. Текст английский по той же причине, что и
     * у драйверов: на русской Windows кириллица в консоли нечитаема.
     */
    protected static void skipOnboarding() {
        By skipButton = id("org.wikipedia:id/fragment_onboarding_skip_button");
        By searchContainer = id("org.wikipedia:id/search_container");

        Wait().withMessage("neither the onboarding screen nor the Wikipedia main screen showed up")
                .until(driver -> !driver.findElements(skipButton).isEmpty()
                        || !driver.findElements(searchContainer).isEmpty());

        if (!getWebDriver().findElements(skipButton).isEmpty()) {
            $(skipButton).click();
        }
    }

    @AfterEach
    void closeApp() {
        // Видео пишет только BrowserStack, и ссылка на него живёт в его же API,
        // поэтому для локальных стендов шага нет — там нечего прикладывать.
        boolean videoAvailable = BrowserstackStand.class.isAssignableFrom(driver());
        String sessionId = videoAvailable ? Selenide.sessionId().toString() : null;

        try {
            // Скриншот и вёрстку снимаем до закрытия сессии: после закрытия драйвер уже недоступен
            Attach.screenshotAs("Last screenshot");
            Attach.pageSource();
        } catch (WebDriverException e) {
            // Сессия могла умереть посреди теста — тогда снимать вложения не с чего.
            // Раньше это исключение вылетало из @AfterEach раньше closeWebDriver(): драйвер
            // оставался привязан к потоку до конца JVM, к падению теста добавлялось второе,
            // из тира-дауна, а в отчёт не попадало ничего. Поэтому закрытие ушло в finally,
            // а причина — во вложение, чтобы её было видно в отчёте.
            Attach.attachAsText("Вложения не сняты", e.getMessage());
        } finally {
            closeWebDriver();
        }

        if (videoAvailable) {
            Attach.addVideo(sessionId);
        }
    }
}
