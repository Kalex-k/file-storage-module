# File Storage Service

Независимый сервис файлового хранилища на основе MinIO и PostgreSQL с контролем доступа, квотами и поддержкой bulk-операций.

## Обзор

Полнофункциональный сервис для управления файлами с контролем доступа на основе ролей, ограничениями по размеру хранилища и поддержкой массовых операций.

### Основные возможности

- Загрузка файлов с валидацией размера и типа
- Скачивание файлов с контролем доступа
- Генерация presigned URLs для прямого доступа к файлам
- Удаление файлов с проверкой прав
- Массовая загрузка файлов (bulk upload)
- Пагинация списка файлов проекта
- Контроль квот хранилища на уровне проекта
- Определение MIME-типов файлов (Apache Tika)
- Классификация файлов по типам
- Блокировка опасных расширений файлов

## Архитектура

```
┌─────────────────┐
│ ResourceController │  ← REST API Layer
└────────┬──────────┘
         │
         ▼
┌─────────────────┐
│ FileStorageService │  ← Business Logic Layer
└────────┬──────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐  ┌──────────┐
│ MinIO  │  │PostgreSQL│  ← Storage Layer
└────────┘  └──────────┘
```

### Слои приложения

1. **Controller Layer** - обработка HTTP запросов, валидация, формирование ответов
2. **Service Layer** - бизнес-логика, контроль доступа, управление квотами
3. **Repository Layer** - абстракция доступа к данным
4. **Storage Layer** - MinIO для файлов, PostgreSQL для метаданных

Подробнее: [ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Технологии

- **Java 17** + **Spring Boot 3.0.6**
- **PostgreSQL** - метаданные и транзакции
- **MinIO** - S3-совместимое объектное хранилище
- **Apache Tika** - определение MIME-типов по содержимому
- **Liquibase** - управление миграциями БД

### Ключевые архитектурные решения

**MinIO** - S3-совместимое API, простота развертывания, self-hosted решение  
**Apache Tika** - точное определение MIME-типов по содержимому, защита от подмены расширений  
**PostgreSQL** - ACID гарантии, эффективные индексы, pessimistic locking для квот  
**Pessimistic Locking** - предотвращение race conditions при параллельных загрузках

Подробнее: [ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Быстрый старт

### Docker Compose (рекомендуется)

```bash
# Запуск PostgreSQL и MinIO
docker-compose up -d

# Проверка статуса
docker-compose ps
```

После запуска:
- **PostgreSQL**: `localhost:5432` (user: `postgres`, password: `postgres`)
- **MinIO API**: `http://localhost:9000`
- **MinIO Console**: `http://localhost:9001` (user: `minioadmin`, password: `minioadmin`)

### Запуск приложения

```bash
# С использованием Gradle
./gradlew bootRun

# Или соберите JAR
./gradlew build
java -jar build/libs/filestorage-service.jar
```

Приложение будет доступно на `http://localhost:8080`

Подробнее: [DEPLOYMENT.md](docs/DEPLOYMENT.md)

## API Endpoints

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/v1/projects/{projectId}/resources` | Загрузка файла |
| GET | `/api/v1/projects/{projectId}/resources/{resourceId}/download` | Скачивание файла |
| GET | `/api/v1/projects/{projectId}/resources/{resourceId}/url` | Получение presigned URL |
| DELETE | `/api/v1/projects/{projectId}/resources/{resourceId}` | Удаление файла |
| GET | `/api/v1/projects/{projectId}/resources` | Список файлов (с пагинацией) |
| POST | `/api/v1/projects/{projectId}/resources/bulk` | Массовая загрузка |

### Обязательные заголовки

Все запросы требуют заголовок: `x-user-id: {userId}`

### Пример загрузки

```bash
curl -X POST http://localhost:8080/api/v1/projects/1/resources \
  -H "x-user-id: 1" \
  -F "file=@document.pdf" \
  -F "allowedRoles=MANAGER,DEVELOPER"
```

## Безопасность

### Ролевая модель доступа

- Файлы могут быть ограничены определенными ролями при загрузке
- Проверка доступа при скачивании (членство в проекте + роль)
- Удаление доступно только создателю файла или менеджеру проекта

### Валидация файлов

- Максимальный размер: 500MB (настраивается)
- Блокировка расширений: `exe`, `bat`, `cmd`, `sh`
- Определение MIME-типа через Apache Tika (анализ содержимого)
- Санитизация имен файлов

Подробнее: [SECURITY.md](docs/SECURITY.md)

## Конфигурация

Основные настройки в `application.yaml`:

```yaml
file-storage:
  max-file-size: 500000000  # 500MB
  blocked-extensions: exe,bat,cmd,sh
  presigned-url-expiry-seconds: 3600
  bulk-upload-max-files: 10

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: filestorage
```

Все значения могут быть переопределены через переменные окружения.

## Дополнительная документация

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - архитектура и дизайн-решения
- [SECURITY.md](docs/SECURITY.md) - безопасность и контроль доступа
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) - развертывание и инфраструктура

## Разработка

### Требования

- Java 17+
- PostgreSQL 12+
- MinIO (через docker-compose)

### Сборка

```bash
./gradlew build
```

### Запуск тестов

```bash
./gradlew test
```

## Лицензия

Этот проект создан для демонстрации функциональности файлового хранилища.

---

**Версия:** 1.0
