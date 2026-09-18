package drivers;

import config.LocalStandConfig;
import io.appium.java_client.android.options.UiAutomator2Options;

import static config.Project.emulation;

/**
 * Стенд {@code -DdeviceHost=emulation}: Appium на своей машине и эмулятор из Device Manager.
 * Всё, кроме выбора устройства, — в {@link LocalAppiumDriver}.
 */
public class EmulationDriver extends LocalAppiumDriver {

    @Override
    protected LocalStandConfig config() {
        return emulation;
    }

    @Override
    protected void selectDevice(UiAutomator2Options options) {
        // Пустое имя оставляем как есть: тогда Appium возьмёт единственное
        // запущенное устройство, и стенд работает без обязательной настройки.
        if (!emulation.avd().isBlank()) {
            options.setAvd(emulation.avd());
        }
    }
}
