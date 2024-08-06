# translation-app
Приложение для перевода слов на другой язык с использованием сервиса перевода apicase.ru. Подробнее о сервисе: https://apicase.ru/api/
## Настройка параметров и запуск приложения
После выкачивания репозитория локально в файле src/main/resources/application.properties необходимо указать параметры подключения для работы приложения:

- spring.datasource.* - адрес БД для хранения результатов запросов, пользователь и пароль для работы с базой
- apicase.translate.api.key - токен для работы с сервисом apicase.ru (можно использовать тестовый токен: demo)
- server.port - порт, на котором будет запущено приложение

База данных для хранения истории запросов должна быть развернута и иметь таблицу со структурой:
```bash
CREATE TABLE translation_db.requests_log (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    ip_address VARCHAR(100),
    input_lang VARCHAR(100),
    input_text VARCHAR(1000),
    output_lang VARCHAR(100),
    output_text VARCHAR(1000),
    error_msg VARCHAR(1000),
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Для компиляции приложения в локальном репозитории необходимо выполнить команды Мавена:
```bash
mvn validate compile package install
```
В подпапке target проекта появится исполняемый файл translation-app-0.0.1-SNAPSHOT.jar.
Для его запуска необходимо перейти в подпапку и выполнить команду:
```bash
java -jar translation-app-0.0.1-SNAPSHOT.jar
```
Приложение с сервисом перевода запущено на localhost по указанному в настроечном файле порту и ждет запросы.

## Отправка запросов
Адрес для запросов:
```bash
http://localhost:8081/translate
```
При обращении к приложению необходимо передать три параметра: исходный язык текста, текст для перевода, язык для перевода. Пример тела запроса:
```bash
{
    "inputLanguage": "en",
    "inputText": "Hello",
    "outputLanguage": "de"
}
```
- Можно использовать Postman:
![Запрос1 Postman](https://github.com/user-attachments/assets/481902f2-7200-4078-b934-db1bc9fbd8d5)

- Можно выполнить curl
```bash
curl --location 'http://localhost:8081/translate' --header 'Content-Type: application/json' --data '{"inputLanguage": "en", "inputText": "Girl loves cats", "outputLanguage": "de"}'
```
При запуске из командной строки Windows необходимо явно указать curl.exe и в запросе экранировать двойные кавычки:

```bash
C:\Windows\System32\curl.exe --location "http://localhost:8081/translate" --header "Content-Type: application/json" --data '{\"inputLanguage\": \"en\",\"inputText\":\"Girl loves cats\",\"outputLanguage\": \"de\"}'
```
![Запрос1 PowerShell](https://github.com/user-attachments/assets/dcc64d8b-ee29-4016-b1c7-ca7c90555663)
