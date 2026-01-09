# Multi-stage build для оптимизации размера образа
FROM gradle:7.6.1-jdk17 AS build

WORKDIR /app

# Копируем файлы конфигурации Gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN gradle build -x test --no-daemon

# Финальный образ
FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем собранный JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
