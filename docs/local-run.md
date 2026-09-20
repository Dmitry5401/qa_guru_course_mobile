# Локальный прогон: эмулятор или телефон вместо BrowserStack

Тесты в `src/test/java/tests/local` идут через `drivers/LocalDriver` на Appium, поднятый на своей
машине. Документ фиксирует, обо что споткнулся первый вариант драйвера и почему настройки лежат
в `local.properties`. Всё проверено 17 сентября 2026 года на Appium 2.19.0 с драйвером
uiautomator2 4.2.3 и на Appium 3.7.0 с uiautomator2 8.7.0.

## Что нужно на машине

```bash
npm install -g appium@2
appium driver install uiautomator2
appium --base-path /wd/hub  # адрес должен совпадать с LOCAL_APPIUM_URL, см. грабли №1
adb devices                 # эмулятор или телефон должны быть в списке как device
```

Версия драйвера и версия сервера связаны жёстко, и `appium driver list --installed` показывает
только драйвер, поэтому пару стоит сверять: `uiautomator2` ветки 4.x требует Appium 2,
а 5.x и выше — Appium 3. Поставить драйвер к чужой мажорной версии сервера Appium не даст.

Обе пары с нашим стеком работают — проверял прогоном одного и того же теста: и
Appium 2.19.0 с `uiautomator2@4.2.3`, и Appium 3.7.0 с `uiautomator2@8.7.0` принимают сессию
от java-client 8.3.0 одинаково. Так что если у вас Appium 3 — понижать его незачем.

Ещё Appium нужен путь до Android SDK — переменная `ANDROID_HOME` (в Windows задаётся
в свойствах системы, после чего терминал надо перезапустить, иначе процесс её не унаследует).
Без неё сессия падает мгновенно, за десятки миллисекунд:
`Neither ANDROID_HOME nor ANDROID_SDK_ROOT environment variable was exported`.

Проверить окружение целиком — `appium driver doctor uiautomator2`. Он же ловит незаданную
`JAVA_HOME`, которая нужна драйверу для подписи приложения:

```
WARN Doctor  ✖ ANDROID_HOME environment variable is NOT set!
WARN Doctor  ✖ adb, emulator could not be found because ANDROID_HOME is NOT set!
WARN Doctor  ✖ JAVA_HOME environment variable is NOT set!
```

## Если тест падает, а причины не видно

Gradle по умолчанию печатал только `SessionNotCreatedException at ProtocolHandshake.java:148`
без самого сообщения — в `build.gradle` стоял `exceptionFormat "short"`. Теперь стоит `full`,
и текст ошибки виден прямо в консоли. Полный отчёт с page source и скриншотом всё так же
лежит в `build/reports/tests/test/index.html`.

Перед созданием сессии `LocalDriver` дёргает `<LOCAL_APPIUM_URL>status` и на частые случаи
отвечает по-человечески: сервер не запущен либо базовый путь не совпал с конфигом.

### Как читать `SessionNotCreatedException`

Строка стека сразу говорит, дошёл ли запрос до сервера:

| Где упало | Что это значит |
| --- | --- |
| `RemoteWebDriver.java:561`, «Possible causes are invalid address of the remote server» | до Appium не дозвонились: сервер не запущен или порт не тот |
| `ProtocolHandshake.java:148`, «Response code ...» | Appium ответил и отказал, дальше смотрим текст ответа |

Частые отказы самого Appium:

| Текст в `Original error` | Причина |
| --- | --- |
| `Could not find a driver for automationName 'UiAutomator2'` | драйвер не установлен — см. про пару версий выше |
| `Response code 404` сразу, без ожидания | базовый путь сервера не совпал с `LOCAL_APPIUM_URL` |
| `Neither ANDROID_HOME nor ANDROID_SDK_ROOT environment variable was exported` | Appium не видит Android SDK, нужна переменная `ANDROID_HOME` |
| `Could not find a connected Android device in 20000ms` | в `adb devices` нет устройства в состоянии `device` |
| `Unable to find an active device or emulator with OS ...` | `LOCAL_PLATFORM_VERSION` не совпала ни с одним устройством |

### Как читать `Element not found`, у которого внутри `NoSuchSessionException`

```
SearchTests > successfulSearchTest() FAILED
    Element not found {AppiumBy.id: org.wikipedia:id/search_container}
    Timeout: 30 s.
    Caused by: NoSuchSessionException: A session is either terminated or not started
```

Заголовок здесь врёт, и это важно: **локатор ни при чём**. Сессия оборвалась где-то посреди
теста, а Selenide про это не знает и честно ищет элемент все 30 секунд, каждый раз получая
от сервера «такой сессии нет». В отчёт попадает последняя ошибка — отсюда «не найден элемент»
снаружи и настоящая причина в самом хвосте цепочки.

Отличить одно от другого можно по `Caused by`: `NoSuchElementException` — правда про локатор,
`NoSuchSessionException` — про оборвавшуюся сессию. Чтобы это не приходилось выискивать,
`SessionDiagnostics` печатает в консоль отдельную рамку с разбором, как только видит в цепочке
потерю сессии.

Сессия умирает не сама по себе, и искать причину надо в таком порядке:

1. **Консоль сервера Appium.** Он пишет, почему закрыл сессию, — это единственное место, где
   причина названа прямо. Полезные строки: `Shutting down because we waited ... for a command`
   (сработал `newCommandTimeout`), `UiAutomator2 server is not responding`,
   `The instrumentation process cannot be initialized`, `Crash of the instrumentation process`.
2. **`adb devices`.** Телефон, ушедший в сон, и отвалившийся USB-кабель уносят сессию с собой:
   устройство пропадает из списка или становится `offline`. Лечится включённым
   «Не выключать экран» в настройках разработчика и другим портом или кабелем.
3. **Оптимизация батареи для `io.appium.uiautomator2.server`.** Samsung усыпляет фоновые
   приложения и убивает сервер вместе с сессией. Серверу и `io.appium.settings` нужно снять
   ограничения в «Батарея → Ограничения фоновой работы».

## Грабли, на которые наступает локальный запуск

### 1. Базовый путь сервера и `LOCAL_APPIUM_URL` должны совпадать

Самая частая причина `SessionNotCreatedException` с кодом 404. В Appium 1 сервер слушал
`/wd/hub`, в Appium 2 и 3 — корень, и старый адрес на сервере без флагов отдаёт «No route found»:

```
$ curl -s http://localhost:4723/status
{"value":{"ready":true,"message":"The server is ready to accept new connections", ...}}

$ curl -s http://localhost:4723/wd/hub/status
{"status":9,"value":{"error":"unknown command", ...}}
```

Со стороны теста это выглядит так, будто виноваты capabilities, хотя до драйвера запрос вообще
не доходит:

```
[HTTP] --> POST /wd/hub/session {"capabilities":{...}}
[HTTP] No route found for /wd/hub/session
[HTTP] <-- POST /wd/hub/session 404 3 ms - 211
```

Адрес задаётся ключом `LOCAL_APPIUM_URL`. Сейчас в `local.properties` стоит
`http://localhost:4723/wd/hub` — под сервер, поднятый как `appium --base-path /wd/hub`.
Именно так его и надо запускать, иначе пути разъедутся.

Если запускать сервер просто командой `appium`, он слушает корень, и тогда суффикс из
`LOCAL_APPIUM_URL` надо убрать. Работает любой из двух вариантов, важно лишь, чтобы обе стороны
говорили об одном адресе.

Сверить, где сервер на самом деле слушает, можно по его собственному стартовому логу — он прямо
печатает готовые адреса:

```
[Appium] Appium REST http interface listener started on http://0.0.0.0:4723/wd/hub
[Appium] You can provide the following URLs in your client code to connect to this server:
        http://127.0.0.1:4723/wd/hub (only accessible from the same host)
```

Если базовый путь и `LOCAL_APPIUM_URL` разъехались, проверка в `LocalDriver` пробует оба варианта
и называет рабочий адрес:

```
java.lang.IllegalStateException: Appium listens at http://localhost:4723/wd/hub,
but LOCAL_APPIUM_URL points to http://localhost:4723. Fix either side:
put LOCAL_APPIUM_URL=http://localhost:4723/wd/hub/ into local.properties,
or restart the server so it serves http://localhost:4723.
```

Починить можно с любой стороны: либо привести `LOCAL_APPIUM_URL` к адресу, который сервер
действительно слушает, либо перезапустить сервер под адрес из конфига.

### 2. Устройство выбирается по `udid`, а не по `deviceName`

`deviceName` в Appium 2 — просто подпись, на выбор устройства она не влияет. Порядок из
`appium-android-driver`, `getDeviceInfoFromCaps()`:

1. задан `udid` — берётся устройство с этим серийником;
2. иначе задан `platformVersion` — ищется устройство с такой версией ОС;
3. иначе берётся первое устройство из `adb devices`.

Поэтому серийник вида `R58R611HJWD` нужно передавать как `udid`. Это особенно важно, когда
одновременно подняты эмулятор и подключён телефон: иначе тест уйдёт на то устройство, которое
случайно оказалось первым. Серийник кладётся в `LOCAL_DEVICE_UDID`, версия ОС —
в `LOCAL_PLATFORM_VERSION`; оба ключа можно оставить пустыми, тогда возьмётся единственное
подключённое устройство.

### 3. Версия приложения прибита гвоздями

Сборка по rolling-тегу `latest` из GitHub меняется каждый день. Прогон такой сборки
(`50608-alpha-2026-09-16`) на реальном Google Pixel 9 показал, что интерфейс переписан на Compose
и тест по ней не пройдёт:

```
Element not found {AppiumBy.accessibilityId: Search Wikipedia}
```

На первом экране той сборки нет ни `Search Wikipedia`, ни `search_src_text` — только
`ComposeView` без resource-id, онбординг из четырёх экранов и нижняя навигация
`Home / Saved / Search / Activity / More`.

Поэтому `LOCAL_APP_URL` указывает на конкретную сборку `2.7.50450-r-2023-08-15` из официального
архива `releases.wikimedia.org`: у неё классическая View-вёрстка с нужными resource-id, пакет
`org.wikipedia` и `targetSdk 33`, то есть она работает и на свежих Android. Обновлять версию
имеет смысл только вместе с локаторами.

### 4. `releases.wikimedia.org` отвечает 403 на стандартный User-Agent Java

```
java.io.IOException: Server returned HTTP response code: 403
```

Тот же файл `curl` забирает без проблем. Дело в
[User-Agent policy](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy) Wikimedia:
дефолтный `Java/21.0.10` блокируется. `LocalDriver` скачивает APK через `URLConnection`
с явным User-Agent проекта.

Скачанный APK лежит в `apps/` — не в `build/`, чтобы `clean` не стирал его, и не в ресурсах,
чтобы `processTestResources` не копировал 17 МБ на каждую сборку. Каталог в `.gitignore`.

### 5. Локатор по content-desc ломается на телефоне с другим языком

Тест искал строку поиска так:

```java
$(accessibilityId("Search Wikipedia")).click();
```

На реальном Samsung Galaxy A22 это дало `Element not found {AppiumBy.accessibilityId: Search Wikipedia}`
после 30 секунд ожидания. При этом онбординг до этого закрывался нормально, и в логе есть подсказка:
упал единственный локатор по **тексту**, а оба локатора по **resource-id** отработали.

Причина в том, что `content-desc` — это переведённая строка ресурсов, она меняется вместе с языком
интерфейса. Проверено на BrowserStack: один и тот же APK на Samsung Galaxy S23 с Android 13,
разница только в `appium:language`.

| Что ищем | Локаль en | Локаль ru |
| --- | --- | --- |
| `content-desc="Search Wikipedia"` | есть | **нет**, вместо неё «Поиск по Википедии» |
| `resource-id` `search_container` | есть | есть |
| `resource-id` `nav_tab_search` | есть | есть |

Поэтому строка поиска ищется по `org.wikipedia:id/search_container`. Это тот самый кликабельный
`CardView`, и он не зависит от языка устройства:

```java
$(id("org.wikipedia:id/search_container")).click();
```

Тот же прогон с русской локалью проходит целиком: по запросу `Appium` находятся `AppImage`,
`App Inventor` и `Appian Way Productions`.

Общее правило для локального прогона: он идёт на личном устройстве с любым языком системы,
поэтому опираться стоит на `resource-id`. `accessibilityId` уместен там, где идентификатор задан
в коде приложения, а не взят из переводимых ресурсов — так сделано в `IosTextInputTests`
с `Text Input` и `Text Output` в демо-приложении BrowserStack.

Тесты в `src/test/java/tests` на BrowserStack тот же `accessibilityId("Search Wikipedia")`
пока используют и проходят: устройства BrowserStack по умолчанию англоязычные. Но если
когда-нибудь понадобится прогон на неанглийской локали, локаторы там придётся заменить так же.

Кстати, попутно проверялась и отвергнутая гипотеза: карточка поиска лежит первым элементом внутри
RecyclerView ленты, и казалось, что она могла не дождаться загрузки из API Wikimedia. Прогон
с профилем `no-network` это опроверг — карточка отрисовалась за 264 мс и без сети.

### 6. Заголовок статьи нельзя искать только в `android.webkit.WebView`

`ArticleTests` сверяет, что открылась именно выбранная статья. Своего `resource-id` у заголовка
нет: тело статьи рисует WebView, а название приходит из его accessibility-дерева. Сначала
локатор был прибит к самому WebView:

```java
$(xpath("//android.webkit.WebView[@text='Java']")).shouldBe(visible);
```

На Samsung Galaxy A22 это дало `Element not found` при том, что экран статьи открылся —
проверка `page_contents_container` прошла строкой выше. Заполненность `text` у самого узла
WebView зависит от сборки Android System WebView, а она обновляется через Play Store отдельно
от системы, то есть на двух телефонах с одной и той же Android 13 может отличаться.

Название при этом лежит на экране и вторым способом — отдельным `TextView` внутри статьи.
Поэтому спрашиваем любой узел внутри контейнера статьи:

```java
$(xpath("//*[@resource-id='org.wikipedia:id/page_contents_container']//*[@text='" + ARTICLE + "']"))
```

На снятой с устройства вёрстке старый локатор находит 1 узел, новый — 3 (сам WebView,
заголовочный `TextView` и вхождение слова в тексте). Достаточно любого.

Вторая половина той же ошибки — выбор статьи из выдачи. По запросу `Java` Википедия возвращает
ещё `JavaScript`, `JavaServer Pages` и `JavaFX`, а `findBy(text("Java"))` берёт первое вхождение
подстроки, то есть может открыть не ту статью — и тогда сверять заголовок не с чем. Нужен
`exactText`. И само название стоит держать одной константой: когда оно вписано в поиск, в клик
и в проверку по отдельности, правка легко применяется не везде.

### 7. Русский текст в консоли Windows превращается в мусор

Сообщения проверки Appium сначала были на русском, и на русской Windows читались так:

```
java.lang.IllegalStateException: Appium ╤Б╨╗╤Г╤И╨░╨╡╤В http://localhost:4723/wd/hub,
╨░ LOCAL_APPIUM_URL ╤Г╨║╨░╨╖╤Л╨▓╨░╨╡╤В ╨╜╨░ http://localhost:4723.
```

Это UTF-8, прочитанный как кодовая страница OEM 866 — она по умолчанию стоит в консоли русской
Windows. Проверяется в одну строку: `"слушает".encode("utf-8").decode("cp866")` даёт ровно
`╤Б╨╗╤Г╤И╨░╨╡╤В`. Gradle отдаёт текст исключения в UTF-8, не спрашивая кодовую страницу
терминала, так что настройками сборки это не лечится — только настройкой самой консоли
(`chcp 65001`), а требовать её от каждого, кто клонирует репозиторий, не хочется.

Поэтому тексты исключений в драйверах теперь на английском: ASCII читается одинаково в любой
кодировке — в `cmd`, Git Bash, IDE и в логе CI. Комментарии в коде и шаги Allure остались
русскими: их читают в IDE и в HTML-отчёте, где UTF-8 везде.

То же правило касается имён тестов: Gradle печатает их в консоль, так что русский
`@DisplayName` превращается в такую же кашу, причём даже у пропущенных тестов.

```
StandSelectionTest > ╨Т╤Л╨▒╤А╨░╨╜╨╜╤Л╨╣ ╤Б╤В╨╡╨╜╨┤ ╨┤╨░╤С╤В ╤Б╨▓╨╛╨╣ ╨┤╤А╨░╨╣╨▓╨╡╤А SKIPPED
```

Поэтому `@DisplayName` из `StandSelectionTest` убраны: английские имена методов вроде
`selectedStandIsFullyConfigured` описывают проверку не хуже, а в консоли читаются.

Заодно в `build.gradle` зафиксирована кодировка исходников:

```groovy
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}
```

Без этого javac читает файлы в кодировке платформы. На JDK 18+ по умолчанию это UTF-8 и всё
совпадает случайно, а на JDK 17 под Windows — cp1251, и русские имена шагов Allure поехали бы
в отчёте уже на компиляции.

## Онбординг

Appium по умолчанию переустанавливает приложение перед сессией, поэтому онбординг встречает тест
на каждом запуске, и его нужно закрывать явно:

```java
$(id("org.wikipedia:id/fragment_onboarding_skip_button")).click();
```

Обойтись `back()` не получится: на первом экране онбординга он либо не делает ничего, либо
выкидывает из приложения.

## Запуск

```bash
./gradlew test --tests "tests.local.*"
```

Префикс `./` обязателен в Git Bash, PowerShell и любой другой оболочке, кроме `cmd`: только
`cmd` ищет исполняемые файлы в текущем каталоге, остальные — нет. Без префикса получите
`bash: gradlew: command not found`, хотя файл лежит рядом. В `cmd` работает короткое
`gradlew test ...`, потому что там подхватывается `gradlew.bat`.

Любой ключ из `local.properties` переопределяется через `-D`, потому что Owner читает
`system:properties` первым:

```bash
./gradlew test --tests "tests.local.*" -DLOCAL_DEVICE_UDID=R58R611HJWD
```

## Эмулятор вместо телефона

Отдельный тест для виртуального устройства не нужен: сценарий тот же, меняется только
устройство, на которое идёт сессия. Устройство — это параметр запуска, а не часть теста,
поэтому копия `SearchTests` под каждый девайс дала бы дублирование без выгоды. Тем более
локаторы опираются на `resource-id`, а он не зависит ни от языка системы, ни от версии Android.

Эмулятор выбирается по имени из Device Manager:

```bash
./gradlew test --tests "tests.local.*" -DLOCAL_AVD=Pixel_10a
```

Для эмулятора имя удобнее серийника: оно не меняется, а номер в `emulator-5554` зависит
от порядка запуска. Appium вдобавок сам поднимет эмулятор с таким именем, если тот ещё
не запущен, — в логе сервера это видно как `Trying to find 'Pixel_10a' emulator`.

Приоритет при выборе устройства: `LOCAL_AVD`, затем `LOCAL_DEVICE_UDID`, затем
`LOCAL_PLATFORM_VERSION`, иначе первое устройство из `adb devices`. Когда телефон и эмулятор
подключены одновременно, задавать один из первых двух ключей обязательно, иначе тест уйдёт
на то устройство, которое случайно оказалось первым в списке.

Если имя AVD не совпадает с существующим (или эмулятор не может запуститься), Appium ждёт
минуту и отдаёт:

```
Original error: Error getting AVD with retry.
Original error: Condition unmet after 60005 ms. Timing out.
```

Сверить имя — `emulator -list-avds`; оно должно совпадать с тем, что показывает Device Manager.

### Про Android 17 и закреплённую сборку приложения

`LOCAL_APP_URL` указывает на сборку 2023 года с `targetSdk 33`, поэтому первый вопрос —
работает ли она на Android 17. Работает: прогон того же APK на Google Pixel 9 с Android 17
в BrowserStack прошёл целиком, вёрстка та же, `search_container` на месте, по запросу `Appium`
нашлось 6 результатов. Так что для эмулятора с Android 17 ни версию приложения, ни локаторы
менять не нужно.

Это не общее правило, а свойство именно этой сборки: у версии 2017 года (`org.wikipedia.alpha`,
которая используется в тестах на BrowserStack) на Android 16 и новее не находятся элементы
поиска вообще — подробности в [browserstack-driver.md](browserstack-driver.md).
