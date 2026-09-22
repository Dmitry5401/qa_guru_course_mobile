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

## Язык устройства — тоже настройка стенда

Википедия выбирает язык статей по языку устройства, а статьи с одинаковым названием
в разных языковых разделах бывают про разное. Самый наглядный случай — как раз тот, что
проверяет `ArticleTests`:

| Язык | Что за статья «Java» | Раздел внешних ссылок | Ссылка на сайт |
| --- | --- | --- | --- |
| английский | остров в Индонезии | «External links» | «Java Software, Oracle» → `oracle.com/java` |
| русский | язык программирования | «Ссылки» | «Официальный сайт Java» → `java.com` |

Разный не только текст — разные шаги: в английском разделе статью о языке надо выбрать
в выдаче по полному названию «Java (programming language)», потому что первой строкой идёт
остров, а в русском разделе первая строка — уже нужная статья. Один тест обе версии
не покрывает, поэтому язык стенды задают явно, одинаково для всех трёх:

```properties
# browserstack.properties
BROWSERSTACK_ANDROID_LANGUAGE=en
BROWSERSTACK_ANDROID_LOCALE=US

# emulation.properties и real.properties
LOCAL_LANGUAGE=en
LOCAL_LOCALE=US
```

У стенда `real` за это приходится платить: телефон обычно личный, а Appium язык обратно
не возвращает. После сессии драйвер восстанавливает клавиатуру, анимации и hidden-api
policy — языка в этом списке нет, так что телефон останется английским, пока не вернуть
язык руками в настройках. Если это неудобно, ключи гасятся на прогон:

```bash
./gradlew test -DdeviceHost=real -DLOCAL_LANGUAGE= -DLOCAL_LOCALE=
```

Тогда пройдёт всё, кроме `ArticleTests`: остальные тесты ищут элементы по `resource-id`,
а те не переводятся. От языка зависит только `ArticleTests` — он читает саму статью.

## iOS идёт только в облако

Локального стенда для iOS нет — нужен macOS с Xcode, — поэтому `TestBaseIos` переопределяет
выбор драйвера и ключ `deviceHost` на него не влияет.

Одного переопределения мало. Без второй половины прогон `./gradlew test -DdeviceHost=emulation`
всё равно поднимал бы iOS-сессию в облаке: вы просили запуск на своей машине, а тратились
минуты BrowserStack. Поэтому на локальных стендах iOS-тесты не запускаются вовсе —
`TestBaseIos` помечен условием выполнения JUnit:

```java
@ExtendWith(BrowserstackOnlyCondition.class)
public class TestBaseIos extends TestBase {
```

Пропуск, а не падение: тест не сломан, он просто неприменим к выбранному стенду. Причина
видна в отчёте, так что пропуск не выглядит потерянным тестом:

```
tests.IosTextInputTests > enteredTextIsShownInOutputTest()   SKIPPED
Тест идёт только в BrowserStack, а выбран стенд emulation
```

Текст причины русский, в отличие от сообщений драйверов: в консоль он не попадает даже
при `--info`, Gradle печатает голое `SKIPPED`, — а в отчёте Allure с UTF-8 всё в порядке.

Видео сессии при этом определяется не по `deviceHost`, а по самому драйверу: признак висит
на облачных драйверах интерфейсом-меткой `drivers/BrowserstackStand`, и тестовая база
спрашивает тот драйвер, который сама же и выбрала.

```java
boolean videoAvailable = BrowserstackStand.class.isAssignableFrom(driver());
```

Так база вообще не знает про стенды, а ответ остаётся верным и для классов, которые
драйвер переопределяют.

## Проверка без устройства

`tests/StandSelectionTest` проверяет разбор ключа и то, что выбранный стенд собран целиком:
драйвер тот, что ожидается, и его конфиг заполнен. Сессия при этом не поднимается, устройство
не нужно, так что тест идёт на любой машине и в CI. Стенд в него приходит из того же ключа,
что и в остальные тесты, поэтому три прогона с разными значениями покрывают все три стенда.
