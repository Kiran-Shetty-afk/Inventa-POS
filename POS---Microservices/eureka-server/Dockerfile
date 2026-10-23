FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl eureka-server -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV EUREKA_INSTANCE_HOSTNAME=eureka-server
COPY --from=build --chown=10001:10001 /workspace/eureka-server/target/*.jar /app/app.jar
USER 10001:10001
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
