FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp package -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true -Djacoco.skip=true

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/financeiro-api-*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=40"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
