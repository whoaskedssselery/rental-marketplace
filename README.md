# Маркетплейс аренды — КР1

Консольное Java-приложение поверх PostgreSQL. Архитектура: Console UI -> Service -> Repository/JDBC -> PostgreSQL.

## Требования

- JDK 17+
- Maven
- Docker Desktop

## Первый запуск (сделать один раз)

1. Создать файл с настройками (пароль БД хранится только в нём, файл в `.gitignore`) и при желании поменять пароль:
   ```bash
   cp .env.example .env
   ```
   Приложение читает `DB_URL`, `DB_USER`, `DB_PASSWORD` из переменных окружения, а если их нет — из `.env`. `docker compose` берёт `POSTGRES_*` из того же `.env`.
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

## ⚠️ Перед каждым коммитом

В проекте настроен [Spotless](https://github.com/diffplug/spotless) (google-java-format) — `mvn compile` **упадёт**, если код отформатирован не единообразно. Перед коммитом всегда прогоняйте:

```bash
mvn spotless:apply
```

Это автоматически переформатирует все `.java`-файлы под общий стиль. Дополнительно в проекте есть `.editorconfig` — IntelliJ подхватывает его автоматически, ничего настраивать не нужно.

## Как работаем с git

`main` защищён — напрямую туда пушить нельзя, только через Pull Request (правило действует на всех, включая админа репозитория).

Ветки под каждую задачу уже созданы, свою заводить не нужно — просто переключитесь на готовую:

```bash
git fetch
git checkout feature/<ветка-из-таблицы>
```

| Тема | Issue | Ветка |
|---|---|---|
| Пользователи | #3 | `feature/user` |
| Объекты аренды | #4 | `feature/listing` |
| Заявки на аренду | #5 | `feature/rental-request` |
| Поиск | #6 | `feature/search` |
| Сортировка | #7 | `feature/sort` |
| Фильтрация + статистика + экспорт | #8 | `feature/reports-export` |
| README + финальная проверка | #9 | `feature/readme-final` |
| Секреты БД через env-переменные | #10 | `feature/env-secrets` |

1. Переключились на свою ветку (см. таблицу).
2. Коммитите туда, не забывая `mvn spotless:apply` перед коммитом.
3. Открываете PR в `main`, в описании — `Closes #<номер issue>`.
4. Ждёте, пока гляну (или сам прогоняете `mvn compile`/`mvn test` перед PR, чтобы сразу было видно, что всё зелёное).
5. Мержим — ветку после мержа можно удалить.

Если два PR трогают один и тот же файл (например, `ConsoleMenu.java` — с этим уже сталкивались в issue #4/#5) — сливаем по очереди, не одновременно, чтобы не было конфликта прямо в конструкторе.

## Тесты

```bash
mvn test
```

Полная проверка (форматирование Spotless, тесты, линтер Checkstyle — правила в `config/checkstyle.xml`):

```bash
mvn verify
```

## Сброс базы данных

Схема и seed-данные (`db/init.sql`) применяются только при первом запуске контейнера. Если нужно применить изменения заново:

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
