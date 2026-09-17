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

### 6. Русский текст в консоли Windows превращается в мусор

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
