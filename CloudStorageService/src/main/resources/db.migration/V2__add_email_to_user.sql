-- Добавление нового столбца email в таблицу user
ALTER TABLE "user" ADD COLUMN email VARCHAR(255) UNIQUE NOT NULL DEFAULT 'user@example.com';