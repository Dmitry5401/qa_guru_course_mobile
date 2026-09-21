package tests;

import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import java.util.Map;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.appium.java_client.AppiumBy.androidUIAutomator;
import static io.appium.java_client.AppiumBy.id;
import static io.qameta.allure.Allure.step;
import static org.openqa.selenium.By.xpath;

public class ArticleTests extends TestBase {

    /**
     * Тест читает английскую Википедию — язык устройства стенды задают явно, см.
     * * {@code docs/stands.md}.
     * 1 В поиск вводится слово Java. Проверяются результаты поиска.
     * 2 Выбирается статья с текстом Java (programming language). Проверяется, что открылась статья о языке программирования
     * 3 Переход в раздел "External links" и переход на официальный сайт Java
     */
    private static final String ARTICLE = "Java";
    private static final String JAVA_LANGUAGE_ARTICLE = "Java (programming language)";
    /**
     * Подзаголовок статьи о языке. Отличает её от одноимённых статей.
     */
    private static final String LANGUAGE_ARTICLE_DESCRIPTION = "programming language";

    private static final String LINKS_SECTION = "External links";
    private static final String OFFICIAL_SITE_LINK = "Java Software, Oracle";

    /**
     * Адрес, на который ведёт ссылка: страница Java на сайте Oracle.
     */
    private static final String OFFICIAL_SITE_URL = "oracle.com/java";

    private static final String SEARCH_RESULT_TITLE = "org.wikipedia:id/page_list_item_title";
    private static final String ARTICLE_CONTAINER = "org.wikipedia:id/page_contents_container";
    private static final String ACTION_BAR = "org.wikipedia:id/page_actions_tab_container";

    /**
     * Насколько прокручиваем статью за один шаг — доля от высоты области жеста. На Galaxy
     * S23 это около 280 px: достаточно мелко, чтобы не проскочить нужную ссылку.
     */
    private static final double SCROLL_STEP = 0.3;

    /**
     * Предохранитель от бесконечной прокрутки, если ссылку так и не удалось подвести.
     */
    private static final int SCROLL_STEPS_LIMIT = 15;

    @Test
    void openJavaArticleTest() {
        step("Пропуск онбординга", TestBase::skipOnboarding);

        step("Поиск статьи «" + ARTICLE + "»", () -> {
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys(ARTICLE);
        });

        step("Открытие статьи «" + JAVA_LANGUAGE_ARTICLE + "» из результатов поиска", () -> {
            $$(id(SEARCH_RESULT_TITLE)).shouldHave(sizeGreaterThan(0));

            // Строку берём локатором по тексту, а не findBy по коллекции. findBy
            // запоминает номер строки, а выдача догружается: сначала приходят офлайновые
            // совпадения, следом сетевые, и список перерисовывается. Пока Selenide
            // собирался нажать, номер уже означал другую строку — нажатие доставалось
            // первому результату, и открывалась статья про остров Ява.
            $(searchResult(JAVA_LANGUAGE_ARTICLE)).click();
        });

        step("Проверка, что открылась статья о языке программирования", () -> {
            $(id(ARTICLE_CONTAINER)).shouldBe(visible);
            $(inArticle("//*[@text='" + JAVA_LANGUAGE_ARTICLE + "']")).shouldBe(visible);
            $(xpath("//*[@resource-id='pcs-edit-section-title-description']"))
                .shouldHave(text(LANGUAGE_ARTICLE_DESCRIPTION));
        });

        step("Переход в раздел «" + LINKS_SECTION + "» через содержание", () -> {
            $(id("org.wikipedia:id/page_contents")).click();
            $(androidUIAutomator("new UiScrollable(new UiSelector()"
                + ".resourceId(\"org.wikipedia:id/toc_list\"))"
                + ".scrollIntoView(new UiSelector().text(\"" + LINKS_SECTION + "\"))"))
                .click();
        });

        step("Проверка, что раздел «" + LINKS_SECTION + "» открылся", () ->
            $(inArticle("//*[@text='" + LINKS_SECTION + "']")).shouldBe(visible)
        );

        step("Переход по ссылке «" + OFFICIAL_SITE_LINK + "»", () ->
            clickArticleLink(OFFICIAL_SITE_LINK)
        );

        step("Проверка, что открылся официальный сайт Java", () -> {
            $(id(ARTICLE_CONTAINER)).shouldNot(exist);
            $(xpath("//*[contains(@text,'" + OFFICIAL_SITE_URL + "')]")).shouldBe(visible);
        });
    }

    /**
     * Локатор узла внутри тела статьи: тем же xpath в содержании легко поймать не то.
     */
    private static By inArticle(String xpathInside) {
        return xpath("//*[@resource-id='" + ARTICLE_CONTAINER + "']" + xpathInside);
    }

    /**
     * Строка выдачи с точно таким заголовком. Точно таким, а не с вхождением: по «Java»
     * выдача возвращает ещё JavaScript, Java version history и саму «Java» про остров.
     */
    private static By searchResult(String title) {
        return xpath("//*[@resource-id='" + SEARCH_RESULT_TITLE + "'][@text='" + title + "']");
    }

    /**
     * Нажимает ссылку в теле статьи. С обычным {@code $(...).click()} этот шаг не работает
     * по двум причинам, и обе тихие — тест не падает, а идёт дальше по чужому экрану.
     * <p>
     * Первая: текст ссылки лежит в {@code TextView}, а нажатие принимает обёртка вокруг
     * него — тап по самому тексту WebView проглатывает, и статья не меняется. Поэтому
     * локатор спрашивает кликабельного родителя.
     * <p>
     * Вторая: панель действий («Save», «Language», «Contents») висит поверх статьи, и
     * ссылка может оказаться прямо под ней — так и вышло с «{@value #OFFICIAL_SITE_LINK}».
     * Тап тогда достаётся панели: тест открывал список языков вместо сайта Java. Поэтому
     * перед нажатием статья прокручивается, пока ссылка не окажется выше панели.
     */
    private static void clickArticleLink(String linkText) {
        SelenideElement link = $(inArticle("//android.view.View[@clickable='true']"
            + "[.//android.widget.TextView[@text='" + linkText + "']]"));

        for (int step = 0; step < SCROLL_STEPS_LIMIT && !isAboveActionBar(link); step++) {
            scrollArticle();
        }
        link.shouldBe(visible).click();
    }

    private static boolean isAboveActionBar(SelenideElement link) {
        // exists() спрашивает без ожидания: ссылка может быть ещё ниже экрана, и тогда
        // ждать её тридцать секунд бессмысленно — надо прокручивать.
        if (!link.exists()) {
            return false;
        }
        return link.getLocation().getY() + link.getSize().getHeight() < actionBarTop();
    }

    /**
     * Прокручивает статью на один шаг вниз.
     * <p>
     * Жест — именно {@code scrollGesture}: у {@code swipeGesture} есть инерция, и страница
     * уезжала так далеко, что нужная ссылка пропадала с экрана.
     * <p>
     * Область жеста — верхняя половина статьи, и это не украшение. Палец начинает движение
     * у нижней границы области, поэтому если дотянуть её до панели действий, нажатие
     * достанется панели: {@code scrollGesture} возвращал {@code false}, страница стояла на
     * месте, и цикл выше упирался в предохранитель.
     */
    private static void scrollArticle() {
        SelenideElement article = $(id(ARTICLE_CONTAINER));
        int top = article.getLocation().getY();
        ((JavascriptExecutor) getWebDriver()).executeScript("mobile: scrollGesture", Map.of(
            "left", article.getLocation().getX(),
            "top", top,
            "width", article.getSize().getWidth(),
            "height", (actionBarTop() - top) / 2,
            "direction", "down",
            "percent", SCROLL_STEP));
    }

    private static int actionBarTop() {
        return $(id(ACTION_BAR)).getLocation().getY();
    }
}
