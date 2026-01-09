# Развертывание

## Требования

- Java 17+
- PostgreSQL 12+
- MinIO Server (или совместимое S3 хранилище)
- Spring Boot 3.x

## Docker Compose

### Вариант 1: Только зависимости

Запуск PostgreSQL и MinIO для локальной разработки:

```bash
docker-compose up -d
```

**Сервисы:**
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

### Вариант 2: Полный стек

Запуск всего приложения в Docker:

```bash
docker-compose -f docker-compose.full.yml up -d
```

**Сервисы:**
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- Application: `http://localhost:8080`

## Запуск MinIO

### Docker Compose

```yaml
minio:
  image: minio/minio:latest
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  command: server /data --console-address ":9001"
```

### Docker (standalone)

```bash
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

## Инициализация bucket

Bucket создается автоматически при первом запуске приложения через `MinioConfig`.

**Проверка:**
1. Откройте MinIO Console: `http://localhost:9001`
2. Войдите с учетными данными: `minioadmin` / `minioadmin`
3. Убедитесь, что bucket `filestorage` создан

## Миграции базы данных

Миграции выполняются автоматически через Liquibase при старте приложения.

**Проверка:**
```sql
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;
```

## Конфигурация

### Переменные окружения

```bash
# Database
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/filestorage

# MinIO
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=filestorage

# File Storage
export FILE_STORAGE_MAX_FILE_SIZE=500000000
export FILE_STORAGE_BLOCKED_EXTENSIONS=exe,bat,cmd,sh
```

### application.yaml

Основные настройки:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/filestorage
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket-name: ${MINIO_BUCKET:filestorage}

file-storage:
  max-file-size: ${FILE_STORAGE_MAX_FILE_SIZE:500000000}
  blocked-extensions: ${FILE_STORAGE_BLOCKED_EXTENSIONS:exe,bat,cmd,sh}
```

## Сборка и запуск

### Локальная разработка

```bash
# Сборка
./gradlew build

# Запуск
./gradlew bootRun
```

### Production

```bash
# Сборка JAR
./gradlew bootJar

# Запуск
java -jar build/libs/filestorage-service.jar
```

### Docker

```bash
# Сборка образа
docker build -t filestorage-service .

# Запуск
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/filestorage \
  -e MINIO_ENDPOINT=http://minio:9000 \
  filestorage-service
```

## Мониторинг

### Health Checks

```bash
# Проверка здоровья приложения
curl http://localhost:8080/actuator/health
```

### Метрики

```bash
# Просмотр метрик
curl http://localhost:8080/actuator/metrics
```

### Рекомендуемый мониторинг

- Размер хранилища проектов
- Количество загруженных файлов
- Ошибки загрузки/скачивания
- Использование квот
- Время отклика API

## Рекомендации для production окружения

1. Использовать HTTPS для всех соединений
2. Настроить connection pooling для PostgreSQL и MinIO
3. Использовать secrets management для паролей и ключей
4. Настроить логирование в централизованную систему
5. Настроить мониторинг и алертинг
6. Регулярные бэкапы базы данных и MinIO
7. Настроить lifecycle policies для MinIO (удаление старых файлов)
8. Использовать CDN для часто запрашиваемых файлов

## Troubleshooting

### MinIO не подключается

1. Проверьте, что MinIO запущен: `docker ps`
2. Проверьте endpoint в конфигурации
3. Проверьте credentials
4. Проверьте сеть между контейнерами

### Миграции не выполняются

1. Проверьте подключение к БД
2. Проверьте логи приложения
3. Проверьте таблицу `databasechangelog`

### Превышение квоты

1. Проверьте `max_storage_size` в таблице `project`
2. Проверьте текущий `storage_size`
3. Удалите неиспользуемые файлы
