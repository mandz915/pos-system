# Build any module with:  docker build --build-arg MODULE=user-api -t pos/user-api .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY pos-common/pom.xml pos-common/
COPY pos-test-support/pom.xml pos-test-support/
COPY gateway-api/pom.xml gateway-api/
COPY user-api/pom.xml user-api/
COPY inventory-api/pom.xml inventory-api/
COPY sales-api/pom.xml sales-api/
COPY administration-api/pom.xml administration-api/
COPY catalog-api/pom.xml catalog-api/
COPY payment-api/pom.xml payment-api/
COPY customer-api/pom.xml customer-api/
COPY purchasing-api/pom.xml purchasing-api/
COPY reporting-api/pom.xml reporting-api/
COPY notification-api/pom.xml notification-api/
RUN mvn -B -q -DskipTests dependency:go-offline || true
COPY . .
ARG MODULE
RUN mvn -B -q -pl ${MODULE} -am -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
RUN groupadd --system pos && useradd --system --gid pos pos
COPY --from=build /workspace/${MODULE}/target/${MODULE}-*.jar /app/app.jar
USER pos
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
