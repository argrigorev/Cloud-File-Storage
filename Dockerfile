FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/dimploma_project-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]