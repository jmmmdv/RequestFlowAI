FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 spring
USER spring
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
