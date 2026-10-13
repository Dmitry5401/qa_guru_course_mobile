package screens;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static io.appium.java_client.AppiumBy.accessibilityId;

/**
 * Главный экран демо-приложения BrowserStack Sample iOS.
 */
public class IosHomeScreen {

    private final SelenideElement textButton = $(accessibilityId("Text Button"));

    public void openTextInput() {
        textButton.click();
    }
}
