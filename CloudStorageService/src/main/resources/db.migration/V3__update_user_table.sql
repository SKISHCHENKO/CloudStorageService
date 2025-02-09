-- Изменение типа колонки role в таблице user для соответствия Enum
ALTER TABLE "user"
    ALTER COLUMN role TYPE VARCHAR(50) USING role::VARCHAR(50),
    ALTER COLUMN role SET NOT NULL;