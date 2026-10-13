package tests;

import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;

public class ArticleTests extends TestBase {

    /**
     * Тест читает английскую Википедию — язык устройства стенды задают явно, см.
     * {@code docs/stands.md}.
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

    @Test
    void openJavaArticleTest() {
        step("Пропуск онбординга", onboardingScreen::skipIfShown);

        step("Поиск статьи «" + ARTICLE + "»", () ->
                searchScreen.search(ARTICLE)
        );

        step("Открытие статьи «" + JAVA_LANGUAGE_ARTICLE + "» из результатов поиска", () ->
                searchScreen.openResult(JAVA_LANGUAGE_ARTICLE)
        );

        step("Проверка, что открылась статья о языке программирования", () ->
                articleScreen.checkOpened(JAVA_LANGUAGE_ARTICLE, LANGUAGE_ARTICLE_DESCRIPTION)
        );

        step("Переход в раздел «" + LINKS_SECTION + "» через содержание", () ->
                articleScreen.openSection(LINKS_SECTION)
        );

        step("Проверка, что раздел «" + LINKS_SECTION + "» открылся", () ->
                articleScreen.checkSectionOpened(LINKS_SECTION)
        );

        step("Переход по ссылке «" + OFFICIAL_SITE_LINK + "»", () ->
                articleScreen.openExternalLink(OFFICIAL_SITE_LINK)
        );

        step("Проверка, что открылся официальный сайт Java", () -> {
            articleScreen.checkClosed();
            externalBrowserScreen.checkAddressContains(OFFICIAL_SITE_URL);
        });
    }
}
