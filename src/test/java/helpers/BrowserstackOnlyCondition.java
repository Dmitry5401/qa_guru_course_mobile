package helpers;

import config.DeviceHost;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static config.Project.deviceHost;

/**
 * Пропускает тесты, которым нужен BrowserStack, когда выбран локальный стенд.
 * Подключается на класс: {@code @ExtendWith(BrowserstackOnlyCondition.class)}.
 * <p>
 * Так устроены iOS-тесты: локального стенда для iOS в проекте нет, для него нужен
 * macOS с Xcode. Без этой проверки они уходили бы в облако при любом значении
 * {@code -DdeviceHost} — то есть прогон локального стенда всё равно тратил бы минуты
 * BrowserStack, хотя просили запуск на своей машине.
 * <p>
 * Пропуск, а не падение: тест не сломан, он просто неприменим к выбранному стенду.
 * Причина видна в отчёте, поэтому пропуск не выглядит как потерянный тест.
 */
public class BrowserstackOnlyCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        DeviceHost host = deviceHost();
        if (host == DeviceHost.BROWSERSTACK) {
            return ConditionEvaluationResult.enabled("deviceHost=browserstack");
        }
        // Текст уходит только в отчёт: в консоли Gradle печатает голое SKIPPED даже с --info.
        // Поэтому здесь русский, как и в шагах Allure, а не английский, как в драйверах.
        return ConditionEvaluationResult.disabled(
                "Тест идёт только в BrowserStack, а выбран стенд " + host.name().toLowerCase());
    }
}
