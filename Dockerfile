FROM docker.io/gradle:9.2-jdk21 AS build

WORKDIR /app

COPY . .

RUN ./gradlew installDist --no-daemon

FROM docker.io/eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN mkdir -p /app

COPY .env /app/.env
COPY --from=build /app/build/install/bellatrix /app/bellatrix

ENTRYPOINT ["/app/bellatrix/bin/bellatrix"]
