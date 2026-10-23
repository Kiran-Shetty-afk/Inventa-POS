FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl api-gateway -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV EUREKA_URL=http://eureka-server:8761/eureka/
COPY --from=build --chown=10001:10001 /workspace/api-gateway/target/*.jar /app/app.jar
USER 10001:10001
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
