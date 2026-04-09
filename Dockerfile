# eclipse-temurin 21-jre-alpine as of 2026-04-09
FROM eclipse-temurin@sha256:6ad8ed080d9be96b61438ec3ce99388e294af216ed57356000c06070e85c5d5d

RUN apk add --no-cache shadow tzdata

WORKDIR /app

RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

COPY --chown=appuser:appgroup --chmod=550 dependencies/ ./
COPY --chown=appuser:appgroup --chmod=550 spring-boot-loader/ ./
COPY --chown=appuser:appgroup --chmod=550 snapshot-dependencies*/ ./
COPY --chown=appuser:appgroup --chmod=550 application/ ./


ENV TZ=Africa/Nairobi

EXPOSE 8097

USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0"]
CMD ["org.springframework.boot.loader.launch.JarLauncher"]

ARG VERSION
ARG BUILD_DATE
ARG VCS_REF

LABEL org.opencontainers.image.title="Ebikes Africa organizations service"
LABEL org.opencontainers.image.description=""
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/EbikesAfrica254/organizations"
LABEL org.opencontainers.image.revision="${VCS_REF}"
