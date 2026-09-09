package tests;

import config.BrowserstackConfig;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class SearchTests {

    private RemoteWebDriver driver;

    @BeforeEach
    void createDriver() throws MalformedURLException {
        Map<String, Object> browserstackOptions = new HashMap<>();
        browserstackOptions.put("userName", BrowserstackConfig.user());
        browserstackOptions.put("accessKey", BrowserstackConfig.accessKey());
        browserstackOptions.put("appiumVersion", BrowserstackConfig.appiumVersion());
        browserstackOptions.put("deviceName", BrowserstackConfig.device());
        browserstackOptions.put("osVersion", BrowserstackConfig.osVersion());
        browserstackOptions.put("projectName", "First Java Project");
        browserstackOptions.put("buildName", "browserstack-build-1");
        browserstackOptions.put("sessionName", "first_test");

        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability("platformName", "android");
        capabilities.setCapability("appium:automationName", "UiAutomator2");
        // app_url (bs://...) или custom_id уже загруженного в App Automate приложения
        capabilities.setCapability("appium:app", BrowserstackConfig.app());
        capabilities.setCapability("bstack:options", browserstackOptions);

        driver = new RemoteWebDriver(URI.create(BrowserstackConfig.hubUrl()).toURL(), capabilities);
    }

    @AfterEach
    void quitDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void successfulSearchTest() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        WebElement searchElement = wait.until(
                ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Search Wikipedia")));
        searchElement.click();

        WebElement insertTextElement = wait.until(
                ExpectedConditions.elementToBeClickable(
                        AppiumBy.id("org.wikipedia.alpha:id/search_src_text")));
        insertTextElement.sendKeys("Appium");

        List<WebElement> searchResults = wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(
                        AppiumBy.id("org.wikipedia.alpha:id/page_list_item_title"), 0));
        assertFalse(searchResults.isEmpty());
    }
}
