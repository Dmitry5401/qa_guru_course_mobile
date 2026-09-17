# Локальный прогон: эмулятор или телефон вместо BrowserStack

Тесты в `src/test/java/tests/local` идут через `drivers/LocalDriver` на Appium, поднятый на своей
машине. Документ фиксирует, обо что споткнулся первый вариант драйвера и почему настройки лежат
в `local.properties`. Всё проверено 17 сентября 2026 года на Appium 2.19.0 с драйвером
uiautomator2 4.2.3.

## Что нужно на машине

```bash
npm install -g appium@2
appium driver install uiautomator2
appium                      # слушает http://localhost:4723/
adb devices                 # эмулятор или телефон должны быть в списке как device
```

Драйвер uiautomator2 версии 5 и выше требует Appium 3. Под Appium 2 ставится ветка 4.x:
`appium driver install uiautomator2@4.2.3`.

## Четыре грабли, на которые наступает локальный запуск

### 1. Базовый путь `/wd/hub` в Appium 2 больше не существует

Самая частая причина `SessionNotCreatedException` с кодом 404. В Appium 1 сервер слушал
`/wd/hub`, в Appium 2 — корень, и старый адрес отдаёт «No route found»:

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

Адрес задаётся ключом `LOCAL_APPIUM_URL`, по умолчанию `http://localhost:4723/`. Если очень нужен
старый путь, Appium умеет его вернуть: `appium --base-path /wd/hub`.

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

Любой ключ из `local.properties` переопределяется через `-D`, потому что Owner читает
`system:properties` первым:

```bash
./gradlew test --tests "tests.local.*" -DLOCAL_DEVICE_UDID=R58R611HJWD
```
