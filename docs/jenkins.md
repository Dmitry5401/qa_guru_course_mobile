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
2. **This project is parameterised**, добавить строковые параметры. Имена обязаны совпадать
   с ключами в `test.properties`, иначе Owner их не подхватит:
   `BROWSERSTACK_ANDROID_DEVICE` = `Samsung Galaxy S22 Ultra`,
   `BROWSERSTACK_ANDROID_OS_VERSION` = `12.0`,
   `BROWSERSTACK_IOS_DEVICE` = `iPhone 14`, `BROWSERSTACK_IOS_OS_VERSION` = `16`.
   Параметром можно сделать любой ключ из таблицы ниже, эти четыре — просто самые нужные.
   Версию Android при этом стоит держать около `12.0`: приложение 2017 года на современном
   Android не работает, подробности в `browserstack-driver.md`, раздел 6.
3. **Source Code Management → Git**: `https://github.com/Dmitry5401/qa_guru_course_mobile`.
   В *Branches to build* указать нужную ветку — по умолчанию Jenkins возьмёт `master`, а тесты
   могут лежать в другой.
4. **Build Steps → Execute shell**:

```bash
./gradlew clean test
```

Пробрасывать параметры флагами не нужно: Jenkins отдаёт их шагу как переменные окружения,
а Owner читает их из источника `system:env`.

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

Настройки читает библиотека Owner. Интерфейсов два: `config/AuthConfig` — доступы,
`config/TestConfig` — всё остальное. У каждого три источника:

```java
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:test.properties"
})
```

Приоритет задаётся порядком в списке: системное свойство важнее переменной окружения,
а она важнее файла.

`LoadType.MERGE` здесь обязателен. По умолчанию Owner работает в режиме `FIRST`: берёт первый
источник, который удалось открыть, и остальные не смотрит. `system:properties` открывается
всегда, поэтому без `MERGE` файл просто не читался бы.

Ключи названы как переменные окружения, чтобы одно и то же имя работало во всех трёх
источниках. Полный список — это и есть содержимое двух файлов:

| Ключ | Файл | Значение |
| --- | --- | --- |
| `BROWSERSTACK_USERNAME` | `auth.properties` | логин аккаунта |
| `BROWSERSTACK_ACCESS_KEY` | `auth.properties` | ключ аккаунта |
| `BROWSERSTACK_HUB` | `test.properties` | `https://hub.browserstack.com/wd/hub` |
| `BROWSERSTACK_APPIUM_VERSION` | `test.properties` | `2.0.1` |
| `BROWSERSTACK_ANDROID_APP_ID` | `test.properties` | `WikipediaSample` |
| `BROWSERSTACK_ANDROID_DEVICE` | `test.properties` | `Samsung Galaxy S22 Ultra` |
| `BROWSERSTACK_ANDROID_OS_VERSION` | `test.properties` | `12.0` |
| `BROWSERSTACK_IOS_APP_ID` | `test.properties` | `BStackSampleApp` |
| `BROWSERSTACK_IOS_DEVICE` | `test.properties` | `iPhone 14` |
| `BROWSERSTACK_IOS_OS_VERSION` | `test.properties` | `16` |

Проверка, что связка работает: прогон с переменной окружения
`BROWSERSTACK_IOS_DEVICE="iPhone 14 Pro"` при значении `iPhone 14` в `test.properties` дал
сессию на переопределённом устройстве. Ровно этим путём идут параметры джоба.

```
GET https://api.browserstack.com/app-automate/sessions/be81ebae...138d.json
device      iPhone 14 Pro
os_version  16.3
status      done
```

Если понадобится передать настройку флагом, а не переменной окружения, работает
`-DBROWSERSTACK_ANDROID_DEVICE=...`. За это отвечает строка в `build.gradle`:

```groovy
systemProperties(System.getProperties())
```

Без неё `-D` остались бы в JVM демона Gradle и до тестовой JVM не дошли.

Одна особенность источника `system:env`: пустая строка — это тоже значение. Если в джобе
очистить строковый параметр, в тесты уйдёт пустое устройство, а не значение из файла.
Падает это сразу и заметно, на создании сессии.

## Ключ доступа

Логин и ключ лежат в `src/test/resources/auth.properties`, поэтому джоб заводится без
настройки — это удобно для учебного репозитория, но ключ виден всем, у кого есть доступ к коду.

Менять код для перехода на секреты не нужно: те же имена читаются из окружения, а Jenkins умеет
их подставлять. Достаточно завести **Credentials** типа *Username with password* и в джобе
включить **Use secret text(s) or file(s)** → *Username and password (separated)* с именами
переменных `BROWSERSTACK_USERNAME` и `BROWSERSTACK_ACCESS_KEY`. Значения из окружения перебьют
файл. Сам `auth.properties` в этом случае можно добавить в `.gitignore`.

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
