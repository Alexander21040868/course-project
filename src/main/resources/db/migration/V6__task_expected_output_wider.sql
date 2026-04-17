-- Длинный многострочный эталон, если нет отдельных test_cases
ALTER TABLE tasks ALTER COLUMN expected_output VARCHAR(10000);
