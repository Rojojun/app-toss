buildscript {
    repositories { mavenCentral() }
    dependencies {
        constraints {
            classpath("tools.jackson.core:jackson-core:3.1.7")
            classpath("tools.jackson.core:jackson-databind:3.1.7")
            classpath("org.apache.commons:commons-lang3:3.20.0")
        }
    }
}

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

extra["tomcat.version"] = "11.0.26"
extra["jackson-bom.version"] = "3.1.7"

group = "com.rojojun"
version = "0.0.1-SNAPSHOT"
description = "family-share"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.slack.api:slack-api-client:1.52.0")
    implementation("io.vavr:vavr:1.0.1")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
