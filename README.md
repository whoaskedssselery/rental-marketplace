# Маркетплейс аренды — КР1

Консольное Java-приложение поверх PostgreSQL: пользователи, объекты аренды и заявки на аренду, с бизнес-правилами, поиском, фильтрацией, сортировкой, статистикой и экспортом в Excel.

Архитектура: Console UI -> Service -> Repository/JDBC -> PostgreSQL.

## Требования

- JDK 17+
- Maven
- Docker Desktop

## Первый запуск

1. Создать файл настроек (пароль БД хранится только в нём, файл в `.gitignore`) и при желании поменять пароль:
   ```bash
   cp .env.example .env
   ```
   Приложение читает `DB_URL`, `DB_USER`, `DB_PASSWORD` из переменных окружения, а если их нет — из `.env`. `docker compose` берёт `POSTGRES_*` из того же файла.
2. Поднять базу данных:
   ```bash
   docker compose up -d
   ```
3. Проверить, что контейнер `healthy`:
   ```bash
   docker compose ps
   ```

## Запуск приложения

```bash
mvn compile exec:java
```

Главное меню: пользователи, объекты аренды, заявки, поиск, фильтрация, сортировка, статистика, экспорт данных, список таблиц БД.
Экспорт создаёт файл `rental-marketplace-<дата-время>.xlsx` в текущей папке (три листа: пользователи, объекты аренды, заявки), путь печатается в консоли.

## Проверки

```bash
mvn verify
```

Запускает форматирование (Spotless, google-java-format), тесты, покрытие (JaCoCo) и линтер (Checkstyle, правила в `config/checkstyle.xml`). Отформатировать код автоматически: `mvn spotless:apply`. В `.editorconfig` заданы отступы для IDE.

Интеграционные тесты JDBC используют запущенную БД и пропускаются, если она недоступна. Те же проверки запускает CI (GitHub Actions) на каждый push и pull request.

## Работа с git

`main` защищён: изменения только через Pull Request с зелёным CI. Ветку после мержа удаляем.

## Сброс базы данных

Схема и seed-данные (`db/init.sql`) применяются только при первом запуске контейнера. Чтобы применить изменения заново:

```bash
docker compose down -v
docker compose up -d
```

## Структура проекта

- `model/` — сущности (`User`, `Listing`, `RentalRequest`), enum'ы, интерфейс `Displayable`, `Statistics`
- `repository/` — доступ к БД через JDBC, интерфейс `CrudRepository`, по классу на таблицу
- `service/` — бизнес-логика: `UserService`, `ListingService`, `RentalRequestService` (+ поиск, фильтры, сортировка), `ReportService` (статистика, экспорт)
- `exception/` — собственные исключения
- `ui/` — консольные меню: `ConsoleMenu` (главное), `UserMenu`, `ListingMenu`, `RentalRequestMenu`; `ConsoleInput` (ввод), интерфейс `Menu`
- `util/` — пул соединений HikariCP и настройки (`DatabaseManager`), экспорт в Excel (`ExcelExporter`)

Тесты используют Fake-репозитории из `src/test/java/.../repository/` (без БД).

## ER-диаграмма

`docs/er-diagram.png`
