plugins {
    java
    id("org.springframework.boot") version "3.0.6"
    id("io.spring.dependency-management") version "1.1.0"
    jacoco
}

group = "com.filestorage"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    /** ------------------------------
     * Spring Boot Starters
     * ------------------------------ */
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    /** ------------------------------
     * Database
     * ------------------------------ */
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("com.h2database:h2")

    /** ------------------------------
     * Utils & Logging
     * ------------------------------ */
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    /** ------------------------------
     * Lombok
     * ------------------------------ */
    val lombokVersion = "1.18.34"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    /** ------------------------------
     *  Testcontainers
     * ------------------------------ */
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.17.6"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    /** ------------------------------
     * Unit Tests
     * ------------------------------ */
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")

    /** ------------------------------
     * MinIO Client
     * ------------------------------ */
    implementation("io.minio:minio:8.5.7")

    /** ------------------------------
     * Apache Tika (MIME type detection)
     * ------------------------------ */
    implementation("org.apache.tika:tika-core:2.9.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.bootJar {
    archiveFileName.set("filestorage-service.jar")
}
