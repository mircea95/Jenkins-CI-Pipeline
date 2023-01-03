FROM openjdk:8-jdk-slim-buster
RUN mkdir -p /home/training_licenses
WORKDIR /home/training_licenses
COPY target/*.jar /home/training_licenses
COPY target/classes/application.properties /home/training_licenses/resources
ENTRYPOINT java -jar *.jar
EXPOSE 8080