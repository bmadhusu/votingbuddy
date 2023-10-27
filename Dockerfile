FROM openjdk:8-alpine

COPY target/uberjar/votingbuddy.jar /votingbuddy/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/votingbuddy/app.jar"]
