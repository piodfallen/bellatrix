FROM docker.io/gradle:9.2-jdk21 AS build

WORKDIR /app

COPY . .

RUN ./gradlew installDist --no-daemon

FROM docker.io/amazoncorretto:21

WORKDIR /app

RUN mkdir -p /app/data

COPY --from=build /app/build/install/bellatrix /app/bellatrix
COPY --from=build /app/.env /app/.env

ENTRYPOINT ["/app/bellatrix/bin/bellatrix"]
