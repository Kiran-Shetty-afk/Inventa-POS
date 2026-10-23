FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl billing-analytics-service -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV EUREKA_URL=http://eureka-server:8761/eureka/ \
    ORDER_SALES_SERVICE_URL=http://order-sales-service:8083 \
    INVENTORY_SERVICE_URL=http://inventory-catalog-service:8082 \
    USER_ORG_SERVICE_URL=http://user-org-service:8081
COPY --from=build --chown=10001:10001 /workspace/billing-analytics-service/target/*.jar /app/app.jar
USER 10001:10001
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
