# ── Stage 1: Extract Spring Boot Layers (Layertools) ─────────────────────────
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /extracted
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 2: Hardened & Layered Production Runtime ───────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik 1: Non-Root kullanıcı ve grup (Least Privilege)
RUN addgroup -S appgroup -g 10001 && \
    adduser -S appuser -u 10001 -G appgroup

# Güvenlik 2 & Layer Caching: Katmanları en az değişenden en çok değişene doğru kopyala
COPY --from=extractor --chown=appuser:appgroup /extracted/dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=appuser:appgroup /extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /extracted/application/ ./

# JVM Performans & Bellek Ergonomisi (Fargate 0.5-1.0 vCPU / 1GB RAM optimize G1GC & Container Resource Limits)
ENV JAVA_TOOL_OPTIONS="\
  -XX:+UseG1GC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod"

# Non-root kullanıcıya geçiş
USER appuser:appgroup

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --retries=3 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Doğrudan JarLauncher çalıştırılır (PID 1 olarak Java başlar, SIGTERM sinyalleri yakalanır)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
