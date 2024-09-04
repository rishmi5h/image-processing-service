FROM openjdk:21-jre-slim
EXPOSE 8080
ADD target/imagery.jar imagery.jar
ENTRYPOINT ["java","-jar","/imagery.jar"]