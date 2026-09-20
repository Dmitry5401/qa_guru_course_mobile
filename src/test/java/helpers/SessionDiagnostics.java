package helpers;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Объясняет падения, в которых виновата оборвавшаяся сессия, а не тест.
 * <p>
 * Когда сессия Appium умирает посреди теста, Selenide продолжает искать элемент до конца
 * таймаута, каждый раз получая {@code NoSuchSessionException}, и в итоге сообщает
 * «Element not found». Выглядит это как проблема локатора, хотя локатор до приложения
 * даже не доехал, а настоящая причина лежит в самом конце цепочки исключений. Этот
 * слушатель вытаскивает её наверх и подсказывает, где смотреть дальше.
 * <p>
 * Текст английский: он идёт в консоль, а на русской Windows (кодовая страница 866)
 * кириллица оттуда выходит нечитаемой — та же история, что с сообщениями драйверов.
 */
public class SessionDiagnostics implements TestWatcher {

    private static final String SESSION_LOST = "A session is either terminated or not started";

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (!sessionWasLost(cause)) {
            return;
        }
        System.err.println("""
                --------------------------------------------------------------------------------
                 The Appium session ended while the test was running. Every lookup after that
                 failed with NoSuchSessionException, so the reported "Element not found" is a
                 consequence, not the cause: the locator was never checked against the app.

                 Where to look, in this order:
                   1. the Appium server console - it prints why the session was terminated;
                   2. `adb devices` - a sleeping phone or a dropped USB cable takes the session
                      down with it, and the entry disappears or turns into `offline`;
                   3. battery optimisation for `io.appium.uiautomator2.server` - Samsung puts
                      background apps to sleep and kills the server together with the session.
                --------------------------------------------------------------------------------""");
    }

    private boolean sessionWasLost(Throwable cause) {
        for (Throwable t = cause; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains(SESSION_LOST)) {
                return true;
            }
        }
        return false;
    }
}
