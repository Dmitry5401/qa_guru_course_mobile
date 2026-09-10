# Запуск тестов на Jenkins

Проверено 10 сентября 2026 года: команды из этого документа выполнены в чистом окружении,
параметризация доведена до реальной сессии BrowserStack.

## Что пришлось починить до запуска

В репозитории не было Gradle wrapper — ни `gradlew`, ни `gradle/wrapper/`. Стандартная команда
джоба падала сразу:

```
./gradlew: No such file or directory
```

Мешал и `.gitignore`: строка `gradle/` исключала каталог wrapper целиком, а вернуть файл
отрицанием `!gradle/wrapper/` нельзя — git не может пере-включить файл, если исключён его
родительский каталог. Поэтому паттерн заменён на `gradle/*` с исключением.

Wrapper зафиксирован в репозитории (`gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew`,
`gradlew.bat`), `gradlew` лежит в git с правами `100755`, так что `chmod +x` в джобе не нужен.
Версия Gradle берётся из wrapper, а не из того, что установлено на агенте — на агенте Gradle
можно не иметь вовсе.

## Что нужно от агента

- JDK. Локально сборка идёт на 21, годится любая версия, поддерживаемая Gradle 8.13.
- Доступ в интернет к `hub.browserstack.com` (сессии), `api.browserstack.com` (видео для Allure)
  и `services.gradle.org` (первая загрузка дистрибутива Gradle).
- Плагин Allure Jenkins, если нужен отчёт.

Эмулятор, Appium и Android SDK на агенте не нужны: устройства живут в BrowserStack, а на агенте
выполняется только Java-клиент.

## Вариант 1: Freestyle job

1. **New Item → Freestyle project**.
2. **This project is parameterised**, добавить строковые параметры. Имена свободные, важно лишь
   совпадение с ключами в шаге сборки:
   `BROWSERSTACK_DEVICE` = `Samsung Galaxy S22 Ultra`, `BROWSERSTACK_OS_VERSION` = `12.0`,
   `BROWSERSTACK_IOS_DEVICE` = `iPhone 14`, `BROWSERSTACK_IOS_OS_VERSION` = `16`.
3. **Source Code Management → Git**: `https://github.com/Dmitry5401/qa_guru_course_mobile`.
   В *Branches to build* указать нужную ветку — по умолчанию Jenkins возьмёт `master`, а тесты
   могут лежать в другой.
4. **Build Steps → Execute shell**:

```bash
./gradlew clean test \
    -Dbrowserstack.device="$BROWSERSTACK_DEVICE" \
    -Dbrowserstack.osVersion="$BROWSERSTACK_OS_VERSION" \
    -Dbrowserstack.ios.device="$BROWSERSTACK_IOS_DEVICE" \
    -Dbrowserstack.ios.osVersion="$BROWSERSTACK_IOS_OS_VERSION"
```

5. **Post-build Actions → Allure Report**, в *Results* указать путь `build/allure-results`.

Отдельный `clean` нужен, чтобы Gradle не переиспользовал результаты предыдущей сборки: тесты
ходят в сеть, поэтому кэш задачи `test` для них бесполезен и вреден.

## Вариант 2: Pipeline job

В корне репозитория лежит `Jenkinsfile` с теми же параметрами плюс `TESTS` — фильтром Gradle,
чтобы гонять не весь набор, а например только iOS. Достаточно создать **Pipeline** job,
в *Definition* выбрать *Pipeline script from SCM*, указать репозиторий и ветку.

Публикация отчёта в нём стоит в `post { always }`, а не в `success`: отчёт нужен именно тогда,
когда тесты упали.

## Как параметры доезжают до тестов

Цепочка такая: параметр джоба → `-D` в команде → `BrowserstackConfig`.

Промежуточное звено легко потерять — это строка в `build.gradle`:

```groovy
systemProperties(System.getProperties())
```

Без неё `-D` останутся в JVM демона Gradle и до тестовой JVM не дойдут.

`BrowserstackConfig` читает значения по приоритету: системное свойство важнее переменной
окружения, а она важнее константы в коде. Полный список ключей:

| Свойство | Переменная окружения | Значение по умолчанию |
| --- | --- | --- |
| `browserstack.user` | `BROWSERSTACK_USERNAME` | из кода |
| `browserstack.key` | `BROWSERSTACK_ACCESS_KEY` | из кода |
| `browserstack.app` | `BROWSERSTACK_APP_ID` | `WikipediaSample` |
| `browserstack.device` | `BROWSERSTACK_DEVICE` | `Samsung Galaxy S22 Ultra` |
| `browserstack.osVersion` | `BROWSERSTACK_OS_VERSION` | `12.0` |
| `browserstack.ios.app` | `BROWSERSTACK_IOS_APP_ID` | `BStackSampleApp` |
| `browserstack.ios.device` | `BROWSERSTACK_IOS_DEVICE` | `iPhone 14` |
| `browserstack.ios.osVersion` | `BROWSERSTACK_IOS_OS_VERSION` | `16` |
| `browserstack.appiumVersion` | `BROWSERSTACK_APPIUM_VERSION` | `2.0.1` |
| `browserstack.hub` | `BROWSERSTACK_HUB` | `https://hub.browserstack.com/wd/hub` |

Проверка, что связка работает: прогон с `-Dbrowserstack.ios.device='iPhone 14 Pro'` при значении
`iPhone 14` в конфиге дал сессию на переопределённом устройстве.

```
GET https://api.browserstack.com/app-automate/sessions/7a6a26e4...1b46.json
device      iPhone 14 Pro
os_version  16.3
status      done
```

## Ключ доступа

Сейчас username и access key лежат в коде константами, поэтому джоб заводится без настройки —
это удобно для учебного репозитория, но ключ виден всем.

Менять код для перехода на секреты не нужно: `BrowserstackConfig` уже читает переменные
окружения, а Jenkins умеет их подставлять. Достаточно завести **Credentials** типа
*Username with password* и в джобе включить **Use secret text(s) or file(s)** →
*Username and password (separated)* с именами переменных `BROWSERSTACK_USERNAME` и
`BROWSERSTACK_ACCESS_KEY`. Значения из окружения перебьют константы.

Передавать ключ через `-D` в шаге сборки не стоит: аргументы командной строки видны в логе
консоли и в списке процессов агента.

## Ограничения, о которые можно споткнуться

Тариф аккаунта — Free, в нём 5 параллельных сессий и 5 в очереди
(`GET https://api-cloud.browserstack.com/app-automate/plan.json`). Пока тесты идут
последовательно, это неважно, но если запустить несколько сборок сразу или включить
параллельный запуск в JUnit, лишние сессии встанут в очередь.

Приложения привязаны к аккаунту: `app_url` из чужого аккаунта не подойдёт, а `custom_id`
(`WikipediaSample`, `BStackSampleApp`) переживает повторную загрузку. Если джоб падает с
`BROWSERSTACK_INVALID_APP_CAP`, смотреть надо в `recent_apps` того аккаунта, чьи ключи
подставлены — подробности в `browserstack-driver.md`.

Видео в отчёте Allure — ссылка на BrowserStack, а не файл внутри сборки, поэтому в старых
отчётах воспроизведение зависит от того, доступна ли ещё сессия на их стороне.
