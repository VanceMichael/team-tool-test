FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean test -Dspring.profiles.active=test -DfailIfNoTests=false -DskipTests=false -DincludeScope=test -Duser.home=/tmp/maven -B
