package tests;

import com.codeborne.selenide.WebDriverProvider;
import drivers.BrowserstackIosDriver;
import helpers.BrowserstackOnlyCondition;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * База для iOS-тестов: механика запуска, вложений и закрытия сессии та же, что в
 * {@link TestBase}, меняется только драйвер — сессия поднимается на iPhone.
 * <p>
 * Стенд для iOS всегда один. Ключ {@code -DdeviceHost} на выбор драйвера здесь не влияет:
 * локального стенда для iOS в проекте нет, нужен macOS с Xcode. Поэтому на локальных
 * стендах эти тесты не запускаются вовсе — см. {@link BrowserstackOnlyCondition}.
 */
@ExtendWith(BrowserstackOnlyCondition.class)
public class TestBaseIos extends TestBase {

    @Override
    protected Class<? extends WebDriverProvider> driver() {
        return BrowserstackIosDriver.class;
    }
}
