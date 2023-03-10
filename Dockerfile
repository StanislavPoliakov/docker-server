FROM gradle:7-jdk17 AS builder
COPY --chown=gradle:gradle . /source
WORKDIR /source
RUN gradle buildFatJar --no-daemon

FROM amazoncorretto:17
EXPOSE 8000:8000
RUN mkdir /app
COPY --from=builder /source/build/libs/ /app/
WORKDIR /app/
ENTRYPOINT ["java","-jar","./docker-server.jar"]