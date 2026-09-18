# Стенды: один набор тестов на трёх окружениях

Стенд выбирается ключом командной строки, тесты при этом не меняются:

```bash
./gradlew test -DdeviceHost=browserstack   # устройство в облаке, значение по умолчанию
./gradlew test -DdeviceHost=emulation      # эмулятор на своей машине
./gradlew test -DdeviceHost=real           # телефон, подключённый по USB
```

Регистр и лишние пробелы значения не имеют, а на опечатку ответ приходит сразу и по делу:

```
java.lang.IllegalArgumentException: Unknown deviceHost 'emulator'.
Use -DdeviceHost=browserstack | emulation | real
```

## Что где лежит

У каждого стенда свой драйвер и свой конфиг на Owner:

| Стенд | Драйвер | Конфиг | Файл настроек |
| --- | --- | --- | --- |
| `browserstack` | `drivers/BrowserstackDriver` | `config/BrowserstackConfig` | `browserstack.properties` |
| `emulation` | `drivers/EmulationDriver` | `config/EmulationConfig` | `emulation.properties` |
| `real` | `drivers/RealDriver` | `config/RealConfig` | `real.properties` |

Плюс то, что стендам общее:

| Что | Где | Зачем отдельно |
| --- | --- | --- |
| Логин и ключ BrowserStack | `config/AuthConfig`, `auth.properties` | секреты отделены от настроек: в CI их подставляют из хранилища, а не из репозитория |
| Адрес Appium и приложение | `config/LocalStandConfig`, `local.properties` | у `emulation` и `real` они совпадают, дублировать в двух файлах — значит однажды поправить только один |
| Выбор стенда | `config/ProjectConfig` | источники только `system:properties` и `system:env`: стенд задают в командной строке, файла для него нет |

`EmulationConfig` и `RealConfig` наследуют общие методы от `LocalStandConfig` и подключают
`local.properties` вторым источником после своего:

```java
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:emulation.properties",
    "classpath:local.properties"
})
public interface EmulationConfig extends LocalStandConfig {
```

Owner с `LoadType.MERGE` читает источники по порядку и берёт первое найденное значение,
поэтому свой файл стенда при необходимости перебивает общий, а `-D` перебивает оба.

Драйверы устроены так же: `LocalAppiumDriver` держит всё общее — проверку сервера, скачивание
APK, сборку `UiAutomator2Options`, — а стенд добавляет только выбор устройства.

```java
public class EmulationDriver extends LocalAppiumDriver {
    @Override
    protected void selectDevice(UiAutomator2Options options) {
        if (!emulation.avd().isBlank()) {
            options.setAvd(emulation.avd());
        }
    }
}
```

## Ключи стендов

Любой ключ переопределяется через `-D`, потому что Owner читает `system:properties` первым.
Менять файлы ради разового прогона не нужно.

```bash
./gradlew test -DdeviceHost=emulation -DEMULATION_AVD=Pixel_10a
./gradlew test -DdeviceHost=real -DREAL_DEVICE_UDID=R58R611HJWD
./gradlew test -DdeviceHost=browserstack -DBROWSERSTACK_ANDROID_DEVICE="Google Pixel 9"
```

`EMULATION_AVD` — имя из Device Manager, `REAL_DEVICE_UDID` — серийник из `adb devices`.
Оба можно оставить пустыми, тогда Appium возьмёт единственное подключённое устройство;
задавать их обязательно, когда рядом и эмулятор, и телефон. Подробности и разбор
частых ошибок — в [local-run.md](local-run.md).

## Почему тесты пришлось объединить

До этой доработки переключать было нечего: тесты в `tests` ходили в сборку Wikipedia 2017 года
(`org.wikipedia.alpha`) и искали строку поиска по `accessibilityId`, а тесты в `tests.local` —
в сборку 2023 года (`org.wikipedia`) по `resource-id`. Один и тот же сценарий существовал
в двух экземплярах, и ни один из них не прошёл бы на чужом стенде.

Теперь во всех трёх местах одна сборка `2.7.50450-r-2023-08-15`: локальные стенды скачивают
её с `releases.wikimedia.org`, в BrowserStack тот же файл загружен под custom_id
`WikipediaStable27`. Пакет `org.wikipedia`, локаторы по `resource-id`, каталог `tests.local`
удалён за ненадобностью.

Заодно это починило проверку в `ArticleTests`. У сборки 2017 года тело статьи не загружалось
вообще — Wikimedia отключила Mobile Content Service, — и тест довольствовался заголовком
в тулбаре. В сборке 2023 года статья открывается полностью, поэтому проверяется то, что нужно:
что открылся экран статьи и что это именно запрошенная статья.

## iOS

iOS-тесты уходят в облако при любом значении `deviceHost`: локального стенда для них нет,
нужен macOS с Xcode. `TestBaseIos` для этого переопределяет выбор драйвера.

Из-за этого «есть ли у сессии видео» нельзя решать по `deviceHost` — iOS-прогон с
`-DdeviceHost=emulation` всё равно идёт в BrowserStack и видео у него есть. Признак висит
на самих облачных драйверах, интерфейсом-меткой `drivers/BrowserstackStand`, а тестовая база
спрашивает драйвер:

```java
boolean videoAvailable = BrowserstackStand.class.isAssignableFrom(driver());
```

## Проверка без устройства

`tests/StandSelectionTest` проверяет разбор ключа и то, что выбранный стенд собран целиком:
драйвер тот, что ожидается, и его конфиг заполнен. Сессия при этом не поднимается, устройство
не нужно, так что тест идёт на любой машине и в CI. Стенд в него приходит из того же ключа,
что и в остальные тесты, поэтому три прогона с разными значениями покрывают все три стенда.
