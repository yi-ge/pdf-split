FROM openjdk:14.0.2-jdk-oraclelinux8

MAINTAINER yige a@wyr.me

ADD target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]