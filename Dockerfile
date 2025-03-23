#1. Build app
FROM eclipse-temurin:21-jdk as build

WORKDIR /workspace/app

#copy maven files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY src src

#building application - without tests
RUN ./mvnw package -DskipTests

RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

#Test app
FROM eclipse-temurin:21-jdk as test
#copying application from the previos stage
COPY --from=build /workspace/app /workspace/app
WORKDIR /workspace/app
CMD ["./mvnw", "test"]

#Final app image
FROM eclipse-temurin:21-jre

VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java","-cp","app:app/lib/*","com.remitly.main.RemitlyInternship.RemitlyInternshipApplication"]



