FROM eclipse-temurin:21-jre

ARG JAR_FILE=build/libs/GameMate-1.0.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]