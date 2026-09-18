package drivers;

import config.LocalStandConfig;
import io.appium.java_client.android.options.UiAutomator2Options;

import static config.Project.real;

/**
 * Стенд {@code -DdeviceHost=real}: Appium на своей машине и телефон, подключённый по USB.
 * Всё, кроме выбора устройства, — в {@link LocalAppiumDriver}.
 */
public class RealDriver extends LocalAppiumDriver {

    @Override
    protected LocalStandConfig config() {
        return real;
    }

    @Override
    protected void selectDevice(UiAutomator2Options options) {
        // Пустой серийник оставляем как есть: тогда Appium возьмёт единственное
        // подключённое устройство, и стенд работает без обязательной настройки.
        if (!real.deviceUdid().isBlank()) {
            options.setUdid(real.deviceUdid());
        }
    }
}
