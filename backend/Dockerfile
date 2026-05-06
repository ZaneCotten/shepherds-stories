
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar -x test --no-daemon

RUN mv build/libs/$(ls build/libs | grep -v plain) app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/app.jar app.jar

ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
