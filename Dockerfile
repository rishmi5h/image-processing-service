FROM eclipse-temurin:21.0.4_7-jre-ubi9-minimal
EXPOSE 8080
ADD target/imagery.jar imagery.jar
ENTRYPOINT ["java","-jar","/imagery.jar"]