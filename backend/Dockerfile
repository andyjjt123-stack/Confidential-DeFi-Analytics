# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN ./mvnw -q -DskipTests package

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/*-SNAPSHOT.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java","-jar","/app/app.jar"]
