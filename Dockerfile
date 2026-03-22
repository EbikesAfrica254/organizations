FROM eclipse-temurin:21-jre-alpine@sha256:9f1de3e01a3c43e2f158abf408ec761813da639961bde93427c1ea42a619a09b

RUN apk add --no-cache shadow tzdata

WORKDIR /app

RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

COPY --chown=appuser:appgroup dependencies/ ./
COPY --chown=appuser:appgroup spring-boot-loader/ ./
COPY --chown=appuser:appgroup snapshot-dependencies*/ ./
COPY --chown=appuser:appgroup application/ ./

ENV TZ=Africa/Nairobi

EXPOSE 8097

USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0"]
CMD ["org.springframework.boot.loader.launch.JarLauncher"]

ARG VERSION
ARG BUILD_DATE
ARG VCS_REF

LABEL org.opencontainers.image.title="eBikes Africa organizations service"
LABEL org.opencontainers.image.description=""
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/EbikesAfrica254/organizations"
LABEL org.opencontainers.image.revision="${VCS_REF}"