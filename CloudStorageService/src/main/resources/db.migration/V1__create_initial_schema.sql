-- Создание базы данных (если требуется)
DO
$$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'storagedb') THEN
        CREATE DATABASE storagedb;
    END IF;
END
$$;

\c storageDB; -- Переключаемся в созданную БД (для PostgreSQL)

-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50)
);

-- Создание таблицы файлов
CREATE TABLE IF NOT EXISTS "Files" (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    owner BIGINT NOT NULL,
    filePath VARCHAR(255) NOT NULL,
    date_of_upload TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner) REFERENCES "user"(id) ON DELETE CASCADE
);