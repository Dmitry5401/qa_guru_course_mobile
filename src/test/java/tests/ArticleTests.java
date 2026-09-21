package tests;

import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import java.util.Map;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.appium.java_client.AppiumBy.androidUIAutomator;
import static io.appium.java_client.AppiumBy.id;
import static io.qameta.allure.Allure.step;
import static org.openqa.selenium.By.xpath;

/**
 * Путь от поиска до внешнего сайта: статья «Java» → раздел «Ссылки» → сайт Oracle.
 * <p>
 * Тест читает русскую Википедию, и это не случайность: статья «Java» в английском
 * разделе — про остров в Индонезии, а не про язык программирования. Поэтому язык
 * устройства стенды задают явно, см. {@code docs/stands.md}.
 */
public class ArticleTests extends TestBase {

    /**
     * Что ищем и что ожидаем увидеть заголовком. Одна константа на весь тест: когда
     * название было вписано в три места руками, правка «Sony» на «Java» применилась не
     * везде, и тест падал на сверке заголовка со старым значением.
     */
    private static final String ARTICLE = "Java";

    /** Подзаголовок статьи. Отличает язык программирования от одноимённых статей. */
    private static final String ARTICLE_DESCRIPTION = "язык программирования";

    private static final String LINKS_SECTION = "Ссылки";
    private static final String OFFICIAL_SITE_LINK = "Официальный сайт Java";

    /**
     * Адрес, на который ведёт ссылка. Именно java.com, а не oracle.com/java: на сайт
     * Oracle про Java ведут оба адреса, и в английском разделе стоит второй — но проверять
     * надо тот, что в ссылке русской статьи, иначе тест падает на последнем шаге.
     */
    private static final String OFFICIAL_SITE_URL = "java.com";

    private static final String ARTICLE_CONTAINER = "org.wikipedia:id/page_contents_container";
    private static final String ACTION_BAR = "org.wikipedia:id/page_actions_tab_container";

    /**
     * Насколько прокручиваем статью за один шаг — доля от высоты области жеста. На Galaxy
     * S23 это около 280 px: достаточно мелко, чтобы не проскочить нужную ссылку.
     */
    private static final double SCROLL_STEP = 0.3;

    /** Предохранитель от бесконечной прокрутки, если ссылку так и не удалось подвести. */
    private static final int SCROLL_STEPS_LIMIT = 15;

    @Test
    void openJavaArticleTest() {
        step("Пропуск онбординга", () ->
            $(id("org.wikipedia:id/fragment_onboarding_skip_button")).click()
        );

        step("Поиск статьи «" + ARTICLE + "»", () -> {
            $(id("org.wikipedia:id/search_container")).click();
            $(id("org.wikipedia:id/search_src_text")).sendKeys(ARTICLE);
        });

        step("Переход по первому результату поиска", () ->
            // Заголовок сверяем до нажатия, и именно exactText: по «Java» выдача
            // возвращает ещё JavaScript, JavaServer Pages и Java SE, а вхождение
            // подстроки пустило бы тест в любую из них.
            $$(id("org.wikipedia:id/page_list_item_title"))
                .shouldHave(sizeGreaterThan(0))
                .first()
                .shouldHave(exactText(ARTICLE))
                .click()
        );

        step("Проверка, что открылась статья о языке программирования", () -> {
            $(id(ARTICLE_CONTAINER)).shouldBe(visible);

            // Своего resource-id у заголовка нет: тело статьи рисует WebView. Название
            // приходит к нам либо как text самого WebView, либо как text вложенного
            // TextView — что именно, зависит от сборки Android System WebView на
            // устройстве. Поэтому спрашиваем любой узел внутри контейнера статьи, а не
            // прибиваем локатор к android.webkit.WebView, как было раньше.
            $(inArticle("//*[@text='" + ARTICLE + "']")).shouldBe(visible);

            // Одного названия мало: «Java» называются и статья о платформе, и статья
            // об острове. Подзаголовок под названием — это и есть та приписка
            // «язык программирования», по которой статью выбирали в выдаче.
            $(xpath("//*[@resource-id='pcs-edit-section-title-description']"))
                .shouldHave(text(ARTICLE_DESCRIPTION));
        });

        step("Переход в раздел «" + LINKS_SECTION + "» через содержание", () -> {
            $(id("org.wikipedia:id/page_contents")).click();

            // Содержание — длинный нативный список, и нужный раздел лежит в самом конце,
            // за пределами экрана. UiScrollable прокручивает список до пункта и отдаёт его
            // нам уже видимым; Selenide сам по себе списки не прокручивает.
            $(androidUIAutomator("new UiScrollable(new UiSelector()"
                    + ".resourceId(\"org.wikipedia:id/toc_list\"))"
                    + ".scrollIntoView(new UiSelector().text(\"" + LINKS_SECTION + "\"))"))
                .click();
        });

        step("Проверка, что раздел «" + LINKS_SECTION + "» открылся", () ->
            $(inArticle("//*[@text='" + LINKS_SECTION + "']")).shouldBe(visible)
        );

        step("Переход по ссылке «" + OFFICIAL_SITE_LINK + "»", () ->
            click(articleLink(OFFICIAL_SITE_LINK))
        );

        step("Проверка, что открылся официальный сайт Java", () -> {
            // Внешние ссылки приложение отдаёт браузеру, поэтому признак перехода — что
            // статья с экрана ушла, а адрес виден в адресной строке. Спрашиваем адрес
            // текстом, а не по id браузера: он свой у Chrome, Samsung Internet и остальных.
            $(id(ARTICLE_CONTAINER)).shouldNot(exist);
            $(xpath("//*[contains(@text,'" + OFFICIAL_SITE_URL + "')]")).shouldBe(visible);
        });
    }

    /** Локатор узла внутри тела статьи: тем же xpath в содержании легко поймать не то. */
    private static org.openqa.selenium.By inArticle(String xpathInside) {
        return xpath("//*[@resource-id='" + ARTICLE_CONTAINER + "']" + xpathInside);
    }

    /**
     * Ссылка в теле статьи. Сам текст ссылки лежит в TextView, а нажатие принимает
     * обёртка вокруг него: тап по TextView WebView просто проглатывает, и тест молча
     * продолжается на прежней странице — на это ушло два прогона, пока не стало видно,
     * что статья не сменилась.
     */
    private static SelenideElement articleLink(String linkText) {
        return $(inArticle("//android.view.View[@clickable='true']"
                + "[.//android.widget.TextView[@text='" + linkText + "']]"));
    }

    /**
     * Нажимает ссылку, предварительно подведя её под палец.
     * <p>
     * Панель действий («Сохранить», «Язык», «Содержание») висит поверх статьи, и ссылка
     * может оказаться прямо под ней — так и вышло с «{@value #OFFICIAL_SITE_LINK}». Тап
     * тогда достаётся панели: тест открывал список языков вместо сайта Oracle. Поэтому
     * сначала прокручиваем статью, пока ссылка не окажется выше панели.
     */
    private static void click(SelenideElement link) {
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
