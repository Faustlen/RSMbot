FROM gradle:8.6.0-jdk21-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon --warning-mode all

FROM bellsoft/liberica-runtime-container:jdk-21-slim-musl
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/rsm.jar

ENTRYPOINT ["java", "-jar", "-Xmx1024m", "/app/rsm.jar"]