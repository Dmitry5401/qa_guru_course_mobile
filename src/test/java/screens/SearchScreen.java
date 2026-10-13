package screens;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static io.appium.java_client.AppiumBy.id;
import static org.openqa.selenium.By.xpath;

/**
 * Поиск Wikipedia: карточка на главном экране, поле ввода и выдача.
 */
public class SearchScreen {

    private static final String RESULT_TITLE_ID = "org.wikipedia:id/page_list_item_title";

    private final SelenideElement searchContainer = $(id("org.wikipedia:id/search_container"));
    private final SelenideElement searchInput = $(id("org.wikipedia:id/search_src_text"));
    private final ElementsCollection resultTitles = $$(id(RESULT_TITLE_ID));

    public void search(String query) {
        searchContainer.click();
        searchInput.sendKeys(query);
    }

    public void checkResultsNotEmpty() {
        resultTitles.shouldHave(sizeGreaterThan(0));
    }

    /**
     * Открывает строку выдачи с точно таким заголовком. Точно таким, а не с вхождением:
     * по «Java» выдача возвращает ещё JavaScript, Java version history и саму «Java»
     * про остров.
     */
    public void openResult(String title) {
        checkResultsNotEmpty();
        $(result(title)).click();
    }

    private static By result(String title) {
        return xpath("//*[@resource-id='" + RESULT_TITLE_ID + "'][@text='" + title + "']");
    }
}
