# ── Stage 1: Build & Dependency Caching (Maven + OpenJDK 21) ─────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Bağımlılıkları önbelleğe almak için pom.xml'i önce kopyala
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyala ve optimize JAR paketle
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Minimal & Hardened Runtime (Eclipse Temurin 21 JRE Alpine) ─────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik: Non-root kullanıcı ve grup
RUN addgroup -S appgroup -g 10001 && \
    adduser -S appuser -u 10001 -G appgroup

# Sadece üretilen JAR dosyasını kopyala ve sahipliğini appuser'a ver
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar /app/app.jar

# JVM Performans & Bellek Ergonomisi (Java 21 ZGC & Container Resource Limits)
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Non-root kullanıcıya geçiş yap
USER appuser:appgroup

EXPOSE 8080

# Spring Boot Actuator Sağlık Kontrolü (Health Probe)
HEALTHCHECK --interval=15s --timeout=3s --retries=3 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Uygulamayı başlat
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
