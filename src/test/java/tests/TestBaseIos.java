package tests;

import com.codeborne.selenide.WebDriverProvider;
import drivers.BrowserstackIosDriver;

/**
 * База для iOS-тестов: механика запуска, вложений и закрытия сессии та же, что в
 * {@link TestBase}, меняется только драйвер — сессия поднимается на iPhone.
 */
public class TestBaseIos extends TestBase {

    @Override
    protected Class<? extends WebDriverProvider> driver() {
        return BrowserstackIosDriver.class;
    }
}
