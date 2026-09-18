package config;

import org.aeonbits.owner.Converter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Превращает значение {@code -DdeviceHost} в элемент {@link DeviceHost}.
 * <p>
 * Своими силами Owner конвертирует перечисления через {@code Enum.valueOf}, то есть
 * с учётом регистра: {@code -DdeviceHost=browserstack} падает с «Cannot convert
 * 'browserstack' to DeviceHost». Писать значение капсом ради этого не хочется,
 * поэтому регистр приводим здесь, а на опечатку отвечаем списком допустимых значений.
 */
public class DeviceHostConverter implements Converter<DeviceHost> {

    @Override
    public DeviceHost convert(Method targetMethod, String input) {
        try {
            return DeviceHost.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(DeviceHost.values())
                    .map(host -> host.name().toLowerCase())
                    .collect(Collectors.joining(" | "));
            throw new IllegalArgumentException(
                    "Unknown deviceHost '" + input + "'. Use -DdeviceHost=" + allowed, e);
        }
    }
}
