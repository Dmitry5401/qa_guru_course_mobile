# Мобильные тесты на BrowserStack: почему драйвер устроен именно так

Документ фиксирует, почему в проекте выбраны W3C-capabilities, `AndroidDriver` и настройки в конфиге,
а не привычный по многим примерам `RemoteWebDriver` с плоскими capability. Все утверждения ниже
проверены прогонами на реальном хабе BrowserStack App Automate 9 сентября 2026 года
(Selenide 6.13.0, Selenium 4.8.3, java-client 8.3.0, приложение WikipediaSample).

## С чего началось

Тест падал на создании сессии:

```
org.openqa.selenium.SessionNotCreatedException: Could not start a new session.
Caused by: org.openqa.selenium.WebDriverException:
  [BROWSERSTACK_INVALID_APP_CAP] The app_url/ custom_id/ shareable_id specified in the 'app'
  capability in your test script is invalid.
```

Причина оказалась не в значении `app`, а в смешении двух форматов capabilities: все остальные
capability были плоские (`browserstack.user`, `device`, `os_version`), а приложение передавалось
с W3C-префиксом — `appium:app`. В legacy-режиме BrowserStack читает плоский ключ `app` и префикс
`appium:app` не видит, то есть считает, что приложение не указано вообще.

Проверка на живом хабе, пять вариантов одного и того же теста:

| Формат capabilities | Значение `app` | Результат |
| --- | --- | --- |
| плоские capability | `appium:app` = `bs://sample.app` | `BROWSERSTACK_INVALID_APP_CAP` |
| плоские capability + `platformName` | `appium:app` = `bs://sample.app` | `BROWSERSTACK_INVALID_APP_CAP` |
| плоские capability | `appium:app` = реальный `bs://<hash>` | `BROWSERSTACK_INVALID_APP_CAP` |
| плоские capability | плоский `app` = реальный `bs://<hash>` | сессия создаётся |
| W3C (`bstack:options`) | `appium:app` = реальный `bs://<hash>` | сессия создаётся, тест проходит |

Главный вывод: даже полностью валидный `app_url` отвергается, если рядом стоят legacy-capability.

## 1. W3C-capabilities вместо плоских

Специфичные для BrowserStack параметры собираются в `bstack:options`, параметры Appium идут
с префиксом `appium:`:

```java
Map<String, Object> browserstackOptions = new HashMap<>();
browserstackOptions.put("userName", BrowserstackConfig.user());
browserstackOptions.put("accessKey", BrowserstackConfig.accessKey());
browserstackOptions.put("deviceName", BrowserstackConfig.device());
browserstackOptions.put("osVersion", BrowserstackConfig.osVersion());

UiAutomator2Options options = new UiAutomator2Options();
options.setApp(BrowserstackConfig.app());
options.setCapability("bstack:options", browserstackOptions);
```

Почему не плоские capability, хотя с ними тест тоже проходит:

- Selenium 4.8.3 уже предупреждает про каждую из них при каждом запуске:
  `Support for Legacy Capabilities is deprecated; You are sending the following invalid
  capabilities: [app, browserstack.key, browserstack.user, build, device, name, os_version, project]`.
- Плоские capability уводят сессию на снятый с поддержки протокол JSON Wire. Это видно изнутри
  сессии: кодек Selenium — `org.openqa.selenium.remote.codec.jwp.JsonHttpCommandCodec`,
  а BrowserStack откатывается на Appium 1.22.0 вместо 2.x.
- Поддержку JSON Wire убрали из Selenium в 4.9.0 (`Remove Json Wire Protocol support (#11823)`
  в java/CHANGELOG), а Selenide тянет Selenium транзитивно.

Запас прочности у плоских capability — ровно одна минорная версия Selenide:

| Selenide | Selenium | JSON Wire |
| --- | --- | --- |
| 6.13.0 | 4.8.3 | есть |
| 6.14.0 | 4.9.1 | удалён |
| 6.16.0 | 4.10.0 | удалён |
| 7.0.0 | 4.14.1 | удалён |

Тот же самый драйвер на плоских capability под Selenide 6.14.0 падает, не доходя до хаба:

```
java.lang.IllegalArgumentException: Illegal key values seen in w3c capabilities:
  [app, browserstack.key, browserstack.user, build, device, name, os_version, project]
```

То есть с W3C-форматом обновление Selenide — рутинная операция, а с плоскими capability оно
превращается в переписывание драйвера.

## 2. AndroidDriver вместо RemoteWebDriver

W3C-спецификация убрала эндпоинт «is element displayed», поэтому Selenium реализует его через
JavaScript-атом. В конструкторе `W3CHttpCommandCodec` это буквально:

```java
alias("isElementDisplayed", "executeScript");   // -> POST /session/:sessionId/execute/sync
alias("getElementAttribute", "executeScript");
alias("getPageSource",      "executeScript");
```

В нативном приложении JS-движка нет, и UiAutomator2 отвечает `Method is not implemented`.
Именно на этом падал первый исправленный вариант теста:

```
org.openqa.selenium.UnsupportedCommandException: {"value":{"error":"unknown method",
  "message":"Method is not implemented"}}
Command: [..., isElementDisplayed {id=...}]
Element: [[RemoteWebDriver: on ANDROID] -> accessibility id: Search Wikipedia]
```

Обратите внимание: элемент был найден, упала именно проверка видимости.

`AppiumW3CHttpCommandCodec` из java-client наследуется от селениумовского и возвращает эти три
команды на настоящие эндпоинты Appium, а также переопределяет `alias(...)`, чтобы Selenium не
переклеил их обратно:

```java
defineCommand("getElementAttribute", get("/session/:sessionId/element/:id/attribute/:name"));
defineCommand("isElementDisplayed",  get("/session/:sessionId/element/:id/displayed"));
defineCommand("getPageSource",       get("/session/:sessionId/source"));
```

Этот кодек подключается только при создании драйвера через `AppiumDriver`/`AndroidDriver` —
с `new RemoteWebDriver(...)` его не будет, каким бы правильным ни был URL хаба.

Для Selenide это критично: `$(...).click()` перед кликом ждёт видимости, любое `should`/`shouldBe`
проверяет условия через `isDisplayed()` и `getAttribute()`, а при падении Selenide сохраняет
скриншот и page source. То есть без Appium-кодека не работал бы каждый шаг теста.

Плюс к кодеку `AndroidDriver` даёт мобильный API, которого в Selenium нет:
`activateApp`/`terminateApp`, `pushFile`/`pullFile`, `getContexts` и переключение в вебвью,
управление сетью и геолокацией.

Оговорка: к BrowserStack это отношения не имеет — с локальным Appium-сервером `RemoteWebDriver`
сломался бы точно так же, проблема на стороне клиентского кодека.

## 3. Настройки в конфиге, а не в драйвере

Логин, ключ, `app`, устройство, версия ОС, версия Appium и адрес хаба лежат константами
в `config/BrowserstackConfig`, но каждое значение переопределяется системным свойством
(`-Dbrowserstack.app=...`) или переменной окружения (`BROWSERSTACK_APP_ID` и т.д.).
`./gradlew test` работает без флагов, при этом ключ можно не держать в коде, а устройство —
менять на запуск, не правя драйвер.

Отдельно про `app`. `app_url` действителен только внутри своего аккаунта: `bs://<hash>`,
скопированный из чужого примера, всегда даст `BROWSERSTACK_INVALID_APP_CAP`. Именно на это
и налетел исходный тест. В конфиге хранится `custom_id`, а не `app_url`, потому что `custom_id`
не меняется при перезаливе APK:

```bash
curl -u "USER:ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
  -F "file=@/path/to/app.apk" -F 'data={"custom_id": "WikipediaSample"}'
```

Проверить, что вообще загружено в аккаунт: `GET https://api-cloud.browserstack.com/app-automate/recent_apps`.

## 4. Selenide на нативном приложении

Две настройки отключают браузерные команды, которых в нативном приложении нет:

```java
Configuration.browserSize = null;      // окна браузера в приложении нет
Configuration.pageLoadTimeout = -1;    // pageLoad UiAutomator2 не поддерживает
```

Без второй строки Selenide вызывает `setTimeout {pageLoad=30000}`, получает
`Not implemented yet for pageLoad` и пишет `Failed to set page load timeout` в лог при каждом
запуске. Тест от этого не падает, но лог мусорится. В `WebDriverFactory` вызов пропускается,
если значение отрицательное.

Создание сессии вынесено в `drivers/BrowserstackMobileDriver` — реализацию `WebDriverProvider`,
которую Selenide подключает через `Configuration.browser`. Тест при этом остаётся чистым:
`open()`, `$`, `$$`, `closeWebDriver()`.

Отчётность Selenide на мобильной сессии работает полностью — проверено подменой локатора
на несуществующий:

```
Element not found {AppiumBy.accessibilityId: Search Nowhere}
Screenshot: file:.../build/reports/tests/1788961715766.0.png
Page source: file:.../build/reports/tests/1788961715766.0.html
Timeout: 30 s.
```

## 5. Драйвер закрывается в @AfterEach

Раньше `driver.quit()` стоял последней строкой теста, то есть при падении не вызывался.
Незакрытая сессия не завершается сразу, а висит до таймаута BrowserStack и тратит минуты тарифа:
в истории аккаунта такие сессии видны как `timeout` длиной 136–142 секунды, тогда как штатно
закрытая сессия занимает около 27 секунд и получает `reason: CLIENT_STOPPED_SESSION`.
Поэтому закрытие живёт в `@AfterEach`, а не в теле теста.

## 6. Устройства и версии ОС устаревают

Скопированные из примеров `device`/`os_version` со временем перестают существовать:

```
[BROWSERSTACK_INVALID_DEVICE] Incorrect device name 'google pixel 3' specified for the 'device' capability.
```

На сентябрь 2026 в App Automate самый старый доступный Pixel — Google Pixel 6 (Android 12),
а устройств с Android 9.0 в списке нет вообще. Актуальный список:
`GET https://api-cloud.browserstack.com/app-automate/devices.json`.

## Чек-лист, если сессия не создаётся

1. `BROWSERSTACK_INVALID_APP_CAP` — приложение не видно хабу. Проверить `recent_apps`, убедиться,
   что `app_url`/`custom_id` из своего аккаунта, и что `appium:app` не смешан с плоскими capability.
2. `BROWSERSTACK_INVALID_APP_URL` — значение `app` синтаксически похоже на `bs://...`, но такого
   приложения нет.
3. `BROWSERSTACK_INVALID_DEVICE` — устройства или версии ОС больше нет в списке.
4. `IllegalArgumentException: Illegal key values seen in w3c capabilities` — в проекте
   Selenium 4.9+, а capability остались плоскими.
5. `Method is not implemented` на `isElementDisplayed`, `getAttribute` или `getPageSource` —
   драйвер создан как `RemoteWebDriver` вместо `AndroidDriver`.
