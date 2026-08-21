# ── Stage 1: Build & Dependency Caching (Maven + OpenJDK 21) ─────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Bağımlılık katmanını önbelleğe almak için pom.xml'i önce kopyala
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyala ve katmanlı JAR (Layered JAR) paketle
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Extract Spring Boot Layers (Layertools) ─────────────────────────
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /extracted
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 3: Hardened & Layered Production Runtime ───────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik 1: Non-Root kullanıcı ve grup (Least Privilege)
RUN addgroup -S appgroup -g 10001 && \
    adduser -S appuser -u 10001 -G appgroup

# Güvenlik 2 & Layer Caching: Katmanları en az değişenden en çok değişene doğru kopyala
# 1. Standart harici kütüphaneler (Neredeyse hiç değişmez - Docker cache)
COPY --from=extractor --chown=appuser:appgroup /extracted/dependencies/ ./
# 2. Spring Boot loader sınıfları
COPY --from=extractor --chown=appuser:appgroup /extracted/spring-boot-loader/ ./
# 3. Snapshot bağımlılıklar
COPY --from=extractor --chown=appuser:appgroup /extracted/snapshot-dependencies/ ./
# 4. Uygulama sınıfları (Sadece sizin kodunuz - Saniyeler içinde değişip build olur)
COPY --from=extractor --chown=appuser:appgroup /extracted/application/ ./

# JVM Performans & Bellek Ergonomisi (Java 21 ZGC & Container Resource Limits)
ENV JAVA_TOOL_OPTIONS="\
  -XX:+UseZGC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod"

# Non-root kullanıcıya geçiş
USER appuser:appgroup

EXPOSE 8080

# Spring Boot Actuator Sağlık Kontrolü
HEALTHCHECK --interval=15s --timeout=3s --retries=3 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Doğrudan JarLauncher çalıştırılır (PID 1 olarak Java başlar, SIGTERM sinyalleri yakalanır)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
