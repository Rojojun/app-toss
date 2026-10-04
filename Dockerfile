# 빌드: JDK 25로 실행 가능한 jar 만들기
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 실행: JRE만, 일반 사용자로
FROM eclipse-temurin:25-jre
RUN useradd --system --create-home app
USER app
WORKDIR /home/app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
# 인증서·비밀값은 이미지에 넣지 않고 환경변수/마운트로 받는다 (docs/06-deploy.md)
ENTRYPOINT ["java", "-jar", "app.jar"]
