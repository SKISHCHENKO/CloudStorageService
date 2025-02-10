# Дипломная работа «Облачное хранилище» #
## Описание проекта ##
Задача — разработать REST-сервис. Сервис должен предоставить REST-интерфейс для загрузки файлов и вывода списка уже загруженных файлов пользователя.

Все запросы к сервису должны быть авторизованы. Заранее подготовленное веб-приложение (FRONT) должно подключаться к разработанному сервису без доработок, а также использовать функционал FRONT для авторизации, загрузки и вывода списка файлов пользователя.

## Требования к приложению ##
- Сервис должен предоставлять REST-интерфейс для интеграции с FRONT.  
- Сервис должен реализовывать все методы, описанные в yaml-файле:
- Вывод списка файлов.
- Добавление файла.
- Удаление файла.
- Авторизация.
- Все настройки должны вычитываться из файла настроек (yml).
- Информация о пользователях сервиса (логины для авторизации) и данные должны храниться в базе данных (на выбор студента).

## Требования к реализации ##
- Приложение разработано с использованием Spring Boot.
- Использован сборщик пакетов gradle/maven.
- Для запуска используется docker, docker-compose.
- Код размещён на Github.
- Код покрыт unit-тестами с использованием mockito.
- Добавлены интеграционные тесты с использованием testcontainers.

## Технологический стек/Условия работы приложения ##
- Java 21 и выше
- Spring Boot 3 и выше
- Spring Security (JWT-token 0.12)
- Postgres 16 и выше
- Docker & Docker Compose
- Maven
- Node.js(version 19.7.0-21.0) & npm (for frontend)
- Minio - файловое хранилище, совместимое с API Amazon S3(подключается через докер-контейнер)

## REST API 

### Авторизация

- **POST /login**
    - **Description:** Вход по почте и паролю.
    - **Request Body:**
      ```json
      {
        "login": "string",
        "password": "string"
      }
      ```
    - **Response:**
      ```json
      {
        "auth-token": "string"
      }
      ```

- **POST /users**
    - **Description:** Создание нового пользователя.
    - **Request Body:**
      ```json
      {
        "username": "Test",
        "password": "12345",
        "email": "test@test.ru",
        "role": "ROLE_USER"
      }
      ```
    - **Response:**
  - **Headers:** `Authorization: Bearer <token>`
  - **Response body:** 'Новый пользователь [имя пользователя] создан.'
  - **Response:** HTTP 200 OK

### File Operations

- **GET /list**
    - **Description:** Получение списка файлов для текущего пользователя.
    - **Headers:** `Authorization: Bearer <token>`
    - **Query Parameters:** `userId=string`
    - **Response:**
      ```json
      [
        {
          "id": "string",
          "filename": "string",
          "size": "number",
          "editAt": "string"
        }
      ]
      ```

- **POST /file**
    - **Description:** Загрузка файла.
    - **Headers:** `Authorization: Bearer <token>`
    - **Query Parameters:** `filename=string`
    - **Request Body:** Multipart form-data with file content.
    - **Response:**
      ```json
      {
        "id": "string",
        "filename": "string",
        "userId": "string",
        "size": "number"
      }
      ```

- **DELETE /cloud/file**
    - **Description:** Удаление файла.
    - **Headers:** `Authorization: Bearer <token>`
    - **Query Parameters:** `filename=string`
    - **Response:** HTTP 200 OK

## Установка и запуск

1. **Clone the repository:**
```sh
   git clone https://github.com/your-repository-url.git
   cd your-repository-folder
```
2. **Configure Postgres:**
    * Установите Postgres или запустите докер-контейнер.  
      Пропишите настройки в application.properties
    * spring.datasource.url=jdbc:postgresql://localhost:5432/storagedb 
    * spring.datasource.username=postgres 
    * spring.datasource.password=postgres 
    * spring.datasource.driver-class-name=org.postgresql.Driver
   
3. **Configure Minio**
    * Запустите докер-контейнер Minio на портах 9000:9000  
      Пропишите настройки в application.properties
    * minio.endpoint=http://localhost:9000
    * minio.access-key=minioaccesskey 
    * minio.secret-key=miniosecretkey 
    * minio.bucket-name=storagebucket 
    * minio.region=us-east-1
   
4. **Build the project:**
```sh
   mvn clean install
```
5. **Run the application:**
```sh
   mvn spring-boot:run
```

## Настройка фронтенда

1. **Перейти в папку с файлами.**
2. **Запуск приложения через командную строку:**
```sh
   npm install
   npm run serve
```

3. **В файле .env FRONT (находится в корне проекта) приложения нужно изменить url до backend**
```env
   VUE_APP_BASE_URL=http://localhost:8080
```

## Запуск через Докер

1. **Build Docker image:**
```sh
   docker build -t CloudStorageService .
```

2. **Run Docker container:**
```sh
   docker run -p 8080:8080 CloudStorageService
```

3. **Docker Compose:**
```sh
   docker-compose up --build
```
