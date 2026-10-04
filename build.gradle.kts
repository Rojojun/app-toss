plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

// Spring Boot 4.1.1(현재 최신 패치)이 정한 버전에 알려진 취약점이 있어, 수정된 버전으로 직접 올린다.
// Boot가 새 버전에서 이 값들을 따라잡으면 아래 두 줄은 지운다.
extra["tomcat.version"] = "11.0.26"       // 11.0.24 이하: 폼/다이제스트 인증 우회 등(GHSA)
extra["jackson-bom.version"] = "3.1.7"    // 3.1.6 이하: jackson-core/databind 서비스 거부 등

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
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
