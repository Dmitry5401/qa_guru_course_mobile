package screens;

import com.codeborne.selenide.SelenideElement;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.Map;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.Wait;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.appium.java_client.AppiumBy.androidUIAutomator;
import static io.appium.java_client.AppiumBy.id;
import static org.openqa.selenium.By.xpath;

/**
 * Экран открытой статьи: тело, содержание и ссылки внутри статьи.
 */
public class ArticleScreen {

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

    /** Сколько раз пробуем нажать ссылку, уводящую из приложения. */
    private static final int LINK_TAP_ATTEMPTS = 3;

    /** Сколько ждём, что статья уйдёт с экрана после нажатия такой ссылки. */
    private static final Duration LEAVE_ARTICLE_TIMEOUT = Duration.ofSeconds(15);

    /** Ссылки нет в дереве элементов: она за пределами экрана, WebView её не отдаёт. */
    private static final int LINK_OFF_SCREEN = Integer.MIN_VALUE;

    private final SelenideElement article = $(id(ARTICLE_CONTAINER));
    private final SelenideElement actionBar = $(id(ACTION_BAR));
    private final SelenideElement contentsButton = $(id("org.wikipedia:id/page_contents"));
    private final SelenideElement description =
            $(xpath("//*[@resource-id='pcs-edit-section-title-description']"));

    public void checkOpened(String title, String articleDescription) {
        article.shouldBe(visible);
        $(inArticle("//*[@text='" + title + "']")).shouldBe(visible);
        description.shouldHave(text(articleDescription));
    }

    public void openSection(String section) {
        contentsButton.click();
        $(androidUIAutomator("new UiScrollable(new UiSelector()"
                + ".resourceId(\"org.wikipedia:id/toc_list\"))"
                + ".scrollIntoView(new UiSelector().text(\"" + section + "\"))"))
                .click();
    }

    public void checkSectionOpened(String section) {
        $(inArticle("//*[@text='" + section + "']")).shouldBe(visible);
    }

    public void checkClosed() {
        article.shouldNot(exist);
    }

    /**
     * Нажимает ссылку, которая уводит из приложения, и убеждается, что она сработала.
     * <p>
     * Тап по ссылке внутри WebView иногда проходит впустую: событие до страницы доходит,
     * а перехода не начинается. Молча — статья остаётся на экране, и падает уже проверка
     * следующего шага, по которой не понять, что именно не сработало. Поэтому нажатие
     * повторяется, а если не сработало совсем — сообщение называет, что на экране вместо
     * браузера. Пустой экран вместо браузера бывает и когда на устройстве нет приложения,
     * готового открыть внешнюю ссылку.
     */
    public void openExternalLink(String linkText) {
        for (int attempt = 0; attempt < LINK_TAP_ATTEMPTS; attempt++) {
            clickArticleLink(linkText);
            if (articleLeftTheScreen()) {
                return;
            }
        }
        throw new AssertionError("Tapping the '" + linkText + "' link did not leave the article"
                + " after " + LINK_TAP_ATTEMPTS + " attempts. The screen belongs to " + screenOwner()
                + ". Check that the device has an app to open external links.");
    }

    /**
     * Локатор узла внутри тела статьи: тем же xpath в содержании легко поймать не то.
     */
    private static By inArticle(String xpathInside) {
        return xpath("//*[@resource-id='" + ARTICLE_CONTAINER + "']" + xpathInside);
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
     * ссылка может оказаться прямо под ней. Тап тогда достаётся панели: открывается её
     * диалог вместо перехода по ссылке. Поэтому перед нажатием статья прокручивается,
     * пока ссылка не окажется выше панели, а если подвести её не удалось — нажатия
     * не происходит вовсе. Жест ведь может и не сработать, и тогда прежний код тапал
     * по панели молча: в отчёте это выглядело как падение следующего шага, где про
     * ссылку и панель уже ни слова.
     */
    private void clickArticleLink(String linkText) {
        SelenideElement link = $(inArticle("//android.view.View[@clickable='true']"
                + "[.//android.widget.TextView[@text='" + linkText + "']]"));

        for (int step = 0; step < SCROLL_STEPS_LIMIT && !isAboveActionBar(link); step++) {
            int before = linkTop(link);
            scrollArticle();
            if (linkTop(link) == before && before != LINK_OFF_SCREEN) {
                throw new AssertionError("The article did not move, so the '" + linkText
                        + "' link stays under the action bar (link top " + before
                        + ", action bar top " + actionBarTop() + "). A tap there would go to the"
                        + " action bar, not to the link. See docs/local-run.md, item 9.");
            }
        }

        // Тап под панелью достаётся панели, и это ничем не проявляется: открывается её
        // диалог, статья остаётся на экране, а падает уже следующая проверка. Поэтому
        // не нажимаем вовсе, если подвести ссылку не удалось.
        if (!isAboveActionBar(link)) {
            throw new AssertionError("The '" + linkText + "' link is still not above the action"
                    + " bar after " + SCROLL_STEPS_LIMIT + " scroll steps (link top "
                    + linkTop(link) + ", action bar top " + actionBarTop() + ")."
                    + " See docs/local-run.md, item 9.");
        }
        link.click();
    }

    private boolean articleLeftTheScreen() {
        try {
            Wait().withTimeout(LEAVE_ARTICLE_TIMEOUT)
                    .until(driver -> driver.findElements(id(ARTICLE_CONTAINER)).isEmpty());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private static String screenOwner() {
        WebDriver driver = getWebDriver();
        if (driver instanceof AndroidDriver android) {
            return android.getCurrentPackage() + " (" + android.currentActivity() + ")";
        }
        return driver.getClass().getSimpleName();
    }

    /** Верх ссылки или {@link #LINK_OFF_SCREEN}, если её сейчас нет в дереве элементов. */
    private static int linkTop(SelenideElement link) {
        return link.exists() ? link.getLocation().getY() : LINK_OFF_SCREEN;
    }

    private boolean isAboveActionBar(SelenideElement link) {
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
    private void scrollArticle() {
        int top = article.getLocation().getY();
        ((JavascriptExecutor) getWebDriver()).executeScript("mobile: scrollGesture", Map.of(
                "left", article.getLocation().getX(),
                "top", top,
                "width", article.getSize().getWidth(),
                "height", (actionBarTop() - top) / 2,
                "direction", "down",
                "percent", SCROLL_STEP));
    }

    private int actionBarTop() {
        return actionBar.getLocation().getY();
    }
}
