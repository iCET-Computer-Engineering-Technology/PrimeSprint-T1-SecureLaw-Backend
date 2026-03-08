FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn

RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN useradd -ms /bin/bash spring
USER spring

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app/app.jar"]