package screens;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.By.xpath;

/**
 * Экран браузера устройства, который открывается по внешней ссылке из статьи.
 */
public class ExternalBrowserScreen {

    public void checkAddressContains(String urlPart) {
        SelenideElement address = $(xpath("//*[contains(@text,'" + urlPart + "')]"));
        address.shouldBe(visible);
    }
}
