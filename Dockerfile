# build arguments for dynamic metadata
ARG VERSION=latest
ARG BUILD_DATE
ARG VCS_REF

# build stage: use official Maven image
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /organizations/app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B
RUN java -Djarmode=layertools -jar target/*.jar extract

# runtime stage: Eclipse Temurin JRE on Alpine (very small)
FROM eclipse-temurin:21-jre-alpine

# install shadow for user management
RUN apk add --no-cache shadow tzdata

WORKDIR /app

# create non-root user
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# copy application layers
COPY --from=builder --chown=appuser:appgroup /organizations/app/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /organizations/app/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /organizations/app/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /organizations/app/application/ ./

# set timezone
ENV TZ=Africa/Nairobi

# expose application port
EXPOSE 8086

# switch to non-root user for security
USER appuser

# run the application using spring boot layertools launcher
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]

ARG VERSION
ARG BUILD_DATE
ARG VCS_REF

LABEL org.opencontainers.image.title="eBikes Africa organizations Service"
LABEL org.opencontainers.image.description="Organization Management service for eBikes Africa platform"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/EbikesAfrica254/organizations"
LABEL org.opencontainers.image.revision="${VCS_REF}"