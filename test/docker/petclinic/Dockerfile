FROM openjdk:11-jdk-slim

# Build the Spring "Pet Clinic" sample application
RUN apt-get update \
    && apt-get install -y git

RUN mkdir -p /build \
    && git clone https://github.com/spring-projects/spring-petclinic.git \
    && cd spring-petclinic \
    && ./mvnw package -Dmaven.test.skip=true \
    && mv target/*.jar /build/petclinic.jar