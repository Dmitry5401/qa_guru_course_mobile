pipeline {
    agent any

    parameters {
        string(
            name: 'TESTS',
            defaultValue: '',
            description: 'Что запускать. Пусто — весь набор. Иначе фильтр Gradle, ' +
                'например tests.IosTextInputTests или tests.Article*'
        )
        string(
            name: 'BROWSERSTACK_ANDROID_DEVICE',
            defaultValue: 'Samsung Galaxy S22 Ultra',
            description: 'Android-устройство'
        )
        string(
            name: 'BROWSERSTACK_ANDROID_OS_VERSION',
            defaultValue: '12.0',
            description: 'Версия Android'
        )
        string(
            name: 'BROWSERSTACK_IOS_DEVICE',
            defaultValue: 'iPhone 14',
            description: 'iOS-устройство'
        )
        string(
            name: 'BROWSERSTACK_IOS_OS_VERSION',
            defaultValue: '16',
            description: 'Версия iOS'
        )
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    stages {
        stage('Tests') {
            steps {
                // Параметры джоба Jenkins отдаёт шагу как переменные окружения, а Owner
                // читает их из source system:env — поэтому пробрасывать их через -D не нужно.
                // Имена параметров выше обязаны совпадать с ключами в test.properties.
                sh "./gradlew clean test ${params.TESTS ? "--tests '${params.TESTS}'" : ''}"
            }
        }
    }

    post {
        // отчёт нужен именно тогда, когда тесты упали, поэтому always, а не success
        always {
            junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
            allure includeProperties: false, results: [[path: 'build/allure-results']]
        }
    }
}
