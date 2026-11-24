FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
