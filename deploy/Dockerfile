FROM eclipse-temurin:17-jre

WORKDIR /app
COPY backend/target/memo-assistant-0.1.0.jar /app/memo-assistant.jar

ENV MEMO_DB_PATH=/data/memo.db
VOLUME ["/data"]
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/memo-assistant.jar"]
