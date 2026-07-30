FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x gradlew && ./gradlew --no-daemon help

COPY src src
RUN ./gradlew --no-daemon assemble

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/docker/main/layers/libs ./libs
COPY --from=build /app/build/docker/main/layers/resources ./resources
COPY --from=build /app/build/docker/main/layers/app/application.jar ./application.jar

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "resources:application.jar:libs/*", "br.com.imoveis.Application"]
