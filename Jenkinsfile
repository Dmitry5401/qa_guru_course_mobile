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
            name: 'BROWSERSTACK_DEVICE',
            defaultValue: 'Samsung Galaxy S22 Ultra',
            description: 'Android-устройство'
        )
        string(
            name: 'BROWSERSTACK_OS_VERSION',
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
                // Устройства и версии ОС читает BrowserstackConfig: системное свойство
                // важнее переменной окружения, а она важнее константы в коде
                sh """
                    ./gradlew clean test \
                        ${params.TESTS ? "--tests '${params.TESTS}'" : ''} \
                        -Dbrowserstack.device='${params.BROWSERSTACK_DEVICE}' \
                        -Dbrowserstack.osVersion='${params.BROWSERSTACK_OS_VERSION}' \
                        -Dbrowserstack.ios.device='${params.BROWSERSTACK_IOS_DEVICE}' \
                        -Dbrowserstack.ios.osVersion='${params.BROWSERSTACK_IOS_OS_VERSION}'
                """
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
