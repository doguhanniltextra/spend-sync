# AWS ECS Fargate Üzerinde Prometheus ve Grafana ile Servisleri İzlemek: Bir FinOps ve Production Engineering Rehberi

> **Özet:** Bir mikroservis veya modüler monolit mimarisini AWS ECS Fargate üzerinde çalıştırırken, Datadog veya CloudWatch Managed Prometheus gibi ayda yüzlerce dolar fatura çıkaran SaaS çözümlerine mahkum değilsiniz. Bu rehberde; **Spring Boot 3**, **Prometheus**, **Grafana**, **Amazon EFS** ve **ECS Service Connect** kullanarak aylık sadece **~$1.30** maliyetle, yüksek güvenlikli ve kalıcı (persistent) bir kurumsal observability altyapısının sıfırdan nasıl kurulduğunu, canlıya alınırken karşılaşılan gerçek tuzakları (post-mortem deneyimleriyle) ve mimari püf noktalarını adım adım inceliyoruz.

---

## İçindekiler

1. [Giriş: Observability İkilemi ve FinOps Stratejisi](#1-giriş-observability-ikilemi-ve-finops-stratejisi)
2. [Mimari Tasarım ve Büyük Resim (High-Level Topology)](#2-mimari-tasarım-ve-büyük-resim-high-level-topology)
3. [Adım 1: Uygulama Katmanı — Spring Boot 3 ve Micrometer Entegrasyonu](#3-adım-1-uygulama-katmanı--spring-boot-3-ve-micrometer-entegrasyonu)
4. [Adım 2: Konfigürasyonu Kod Olarak Paketlemek (Docker & ECR)](#4-adım-2-konfigürasyonu-kod-olarak-paketlemek-docker--ecr)
5. [Adım 3: Kalıcı Depolama (EFS) ve TSDB Kilit Çatışması Tuzağı](#5-adım-3-kalıcı-depolama-efs-ve-tsdb-kilit-çatışması-tuzağı)
6. [Adım 4: Servis Keşfi — ALB Olmadan ECS Service Connect ile DNS Çözümleme](#6-adım-4-servis-keşfi--alb-olmadan-ecs-service-connect-ile-dns-çözümleme)
7. [Adım 5: Kurumsal Ağ Güvenliği ve Defense-in-Depth](#7-adım-5-kurumsal-ağ-güvenliği-ve-defense-in-depth)
8. [Adım 6: Yaşanan Canlı Ortam Tuzakları ve Çözümleri (Post-Mortem Çıkarımları)](#8-adım-6-yaşanan-canlı-ortam-tuzakları-ve-çözümleri-post-mortem-çıkarımları)
9. [Adım 7: Panolar ve Canlı Metrik Doğrulaması](#9-adım-7-panolar-ve-canlı-metrik-doğrulaması)
10. [FinOps Dokunuşu: Night Sleep Mode ile Maliyeti Sıfıra Yaklaştırmak](#10-finops-dokunuşu-night-sleep-mode-ile-maliyeti-sıfıra-yaklaştırmak)
11. [Sonuç ve Üretim Ortamı Kontrol Listesi (Checklist)](#11-sonuç-ve-üretim-ortamı-kontrol-listesi-checklist)

---

## 1. Giriş: Observability İkilemi ve FinOps Stratejisi

Bulut bilişim dünyasında en sık karşılaşılan tuzaklardan biri, geliştirme ve erken aşama üretim ortamlarında bile log ve metrik faturalarının ana hesaplama (compute) maliyetini katbekat aşmasıdır:

- **Datadog / New Relic:** Host başına $15-$25/ay + özel metrik başı ücretlendirme.
- **AWS CloudWatch Managed Service for Prometheus (AMP) & Managed Grafana (AMG):** Çalışma alanı başına $9/ay lisans + metrik ingestion + CloudWatch API sorgu maliyetleri (aylık taban $50 - $90+).

Peki ya Kubernetes (EKS) karmaşasına girmeden, AWS'in sunucusuz konteyner altyapısı olan **ECS Fargate Spot** üzerinde kendi Prometheus ve Grafana sistemimizi ayağa kaldırsaydık?

```
┌────────────────────────────────────────────────────────────────────────┐
│                        FinOps Maliyet Kıyaslaması                       │
├───────────────────────────────────────────────────────┬────────────────┤
│ Çözüm                                                 │ Aylık Taban    │
├───────────────────────────────────────────────────────┼────────────────┤
│ Datadog / New Relic (Standart Paket)                  │ $150.00+       │
│ AWS Managed Prometheus (AMP) + Managed Grafana (AMG)  │ $65.00 - $90.00│
│ Self-Hosted ECS Fargate Spot + Amazon EFS (SpendSync) │ ~$1.30         │
└───────────────────────────────────────────────────────┴────────────────┘
```

Bu yazıda, SpendSync platformunda tam olarak bu felsefeyle hayata geçirdiğimiz mimariyi tüm teknik detaylarıyla paylaşıyoruz.

---

## 2. Mimari Tasarım ve Büyük Resim (High-Level Topology)

Mimarimiz şu temel bileşenlerden oluşur:
1. **Spring Boot Backend (ECS Fargate Spot):** `/actuator/prometheus` üzerinden JVM, HikariCP ve HTTP metriklerini dışa aktarır.
2. **Prometheus (ECS Fargate Spot):** Metrikleri çeker (scrape) ve EFS üzerinde tutulan TSDB'ye yazar.
3. **Grafana (ECS Fargate Spot):** Prometheus'u sorgular ve panoları kullanıcıya sunar.
4. **Amazon EFS:** Konteynerler yeniden başlasa bile metriklerin ve kullanıcı dashboard'larının kaybolmamasını sağlayan kalıcı NFS depolama birimi.
5. **ECS Service Connect (Cloud Map):** Dahili Load Balancer maliyeti olmadan servislerin birbirini `backend:8080` ve `prometheus:9090` gibi basit DNS alias'ları üzerinden bulmasını sağlayan Envoy tabanlı servis ağı.

```mermaid
flowchart TD
    DevUser["Geliştirici / DevOps Mühendisi"] -->|HTTP 3000 (IP Whitelist /32)| Grafana["Grafana 11.2 (ECS Fargate Spot)"]
    WebClient["Son Kullanıcı"] -->|HTTPS 443| CloudFront["CloudFront CDN"]
    CloudFront -->|API /api/*| ALB["Application Load Balancer"]
    ALB -->|HTTP 8080| Backend["Spring Boot Backend (ECS Fargate Spot)"]

    subgraph VPC ["AWS VPC (10.0.0.0/16 eu-north-1)"]
        subgraph PublicSubnets ["Public Subnet (2 AZ)"]
            ALB
            Grafana
        end

        subgraph PrivateSubnets ["Private Subnet (2 AZ)"]
            Backend
            Prometheus["Prometheus 2.54 (ECS Fargate Spot)"]
            RDS[("RDS PostgreSQL 16")]
            Valkey[("ElastiCache Valkey")]
            EFS[("Amazon EFS Persistent Storage")]
        end

        subgraph ServiceConnectMesh ["ECS Service Connect Mesh: spendsync-internal"]
            Prometheus -.->|Scrape HTTP backend:8080| Backend
            Grafana -.->|Query HTTP prometheus:9090| Prometheus
        end
    end

    Prometheus -->|Mount /prometheus (UID 65534)| EFS
    Grafana -->|Mount /grafana (UID 472)| EFS
    Backend --> RDS
    Backend --> Valkey
```

---

## 3. Adım 1: Uygulama Katmanı — Spring Boot 3 ve Micrometer Entegrasyonu

Bir Java Spring Boot uygulamasını Prometheus ile izlemek için ilk adım, ham metrikleri Prometheus formatına (`text/plain; version=0.0.4`) dönüştüren Micrometer adaptörünü eklemektir.

### 3.1. Bağımlılıklar (`pom.xml`)
Sadece `spring-boot-starter-actuator` eklemek yetersizdir. Actuator varsayılan olarak JSON üretir; Prometheus metriklerini biçimlendirebilmesi için `micrometer-registry-prometheus` şarttır:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.13.0</version>
</dependency>
```

### 3.2. Yapılandırma (`application.yml`)
Actuator endpoint'lerinin dışa açılması ve histogramların etkinleştirilmesi:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

### 3.3. Spring Security Filtresi (`SecurityConfig.java`)
Geliştiricilerin en sık takıldığı tuzak: Health endpoint'i açıkken Prometheus endpoint'inin 401/403 veya 404 dönmesidir. Spring Security filtre zincirinde açıkça izin verilmelidir:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
    .requestMatchers("/api/v1/auth/**").permitAll()
    .anyRequest().authenticated()
)
```

---

## 4. Adım 2: Konfigürasyonu Kod Olarak Paketlemek (Docker & ECR)

Prometheus ve Grafana'yı ham imaj olarak çekip ECS'e koymak yerine, **Config-as-Code** yaklaşımıyla yapılandırmalarını Dockerfile içine mühürlüyoruz (bake). Böylece runtime'da dinamik dosya indirme riskleri ortadan kalkar.

### 4.1. Prometheus Dockerfile & Konfigürasyonu
Prometheus imajımız, kazıma (scraping) hedefini doğrudan Service Connect DNS alias'ı olan `backend:8080` olarak tanımlar:

```yaml
# monitoring/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spendsync-backend'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
    static_configs:
      - targets: ['backend:8080']
        labels:
          application: 'spendsync-backend'
          environment: 'dev'
```

```dockerfile
# monitoring/prometheus/Dockerfile
FROM prom/prometheus:v2.54.1
COPY prometheus.yml /etc/prometheus/prometheus.yml
EXPOSE 9090
```

### 4.2. Grafana Dockerfile & Provisioning
Grafana açıldığında hiçbir elle konfigürasyon yapmaya gerek kalmadan Prometheus veri kaynağını ve panolarını hazır getirmelidir:

```yaml
# monitoring/grafana/provisioning/datasources/datasources.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

```dockerfile
# monitoring/grafana/Dockerfile
FROM grafana/grafana:11.2.0
COPY provisioning /etc/grafana/provisioning
COPY dashboards /var/lib/grafana/dashboards
EXPOSE 3000
```

Her iki imaj da özel **Amazon ECR** repository'lerine (`spendsync-prometheus`, `spendsync-grafana`) push edilir.

---

## 5. Adım 3: Kalıcı Depolama (EFS) ve TSDB Kilit Çatışması Tuzağı

Fargate konteynerleri doğası gereği geçicidir (ephemeral). Bir görev (task) durdurulduğunda diskindeki veriler uçar. Prometheus'un zaman serisi veritabanını (TSDB) ve Grafana'nın ayarlarını korumak için **Amazon Elastic File System (EFS)** kullanıyoruz.

### 5.1. EFS Access Point ve POSIX İzinleri
Fargate, EFS'e bağlanırken root olmayan kullanıcı izinleriyle çalışır. Eğer Access Point oluşturulmazsa konteyner diske yazamaz (`Permission Denied`):

```hcl
# Prometheus için POSIX User: nobody (UID 65534)
resource "aws_efs_access_point" "prometheus" {
  file_system_id = aws_efs_file_system.monitoring.id

  posix_user {
    gid = 65534
    uid = 65534
  }

  root_directory {
    path = "/prometheus"
    creation_info {
      owner_gid   = 65534
      owner_uid   = 65534
      permissions = "755"
    }
  }
}

# Grafana için POSIX User: grafana (UID 472)
resource "aws_efs_access_point" "grafana" {
  file_system_id = aws_efs_file_system.monitoring.id

  posix_user {
    gid = 472
    uid = 472
  }

  root_directory {
    path = "/grafana"
    creation_info {
      owner_gid   = 472
      owner_uid   = 472
      permissions = "755"
    }
  }
}
```

### 5.2. Hayati Tuzak: Prometheus TSDB Kilit Çatışması (INC-012)
ECS Fargate'te varsayılan rolling deployment kuralı şöyledir:
`maximum_percent = 200`, `minimum_healthy_percent = 100`

Bu kural, yeni versiyon çıkarken önce yeni konteyneri başlatır, o sağlıklı olunca eski konteyneri kapatır. 
**Sorun:** Prometheus'un gömülü TSDB'si, dizin üzerinde tekil bir dosya kilidi tutar (`flock` on `/prometheus/lock`). İki konteyner aynı anda aynı EFS Access Point'e bağlandığında yeni gelen görev anında çöker:
```
tsdb: open /prometheus/lock: resource temporarily unavailable
FAILED: Opening storage failed
```

**Çözüm:** Stateful tekil servisler için deployment stratejisi kesinlikle sınırlandırılmalıdır:
```hcl
deployment_maximum_percent         = 100
deployment_minimum_healthy_percent = 0
```
Böylece ECS önce eski görevi kapatır, NFS dosya kilidi serbest kalır ve yeni görev sorunsuz ayağa kalkar.

---

## 6. Adım 4: Servis Keşfi — ALB Olmadan ECS Service Connect ile DNS Çözümleme

ECS üzerindeki Prometheus, `backend:8080` adresine nasıl erişir? Normalde ya bir dahili Application Load Balancer kurmanız gerekir (aylık ~$18 ekstra fatura) ya da Route 53 Private DNS yönetmeniz gerekir.

AWS, bu problemi çözmek için Envoy sidecar tabanlı **ECS Service Connect** teknolojisini sunar.

### 6.1. Cloud Map HTTP Namespace
```hcl
resource "aws_service_discovery_http_namespace" "internal" {
  name        = "spendsync-internal"
  description = "Service Connect mesh for SpendSync internal service discovery"
}
```

### 6.2. Task Definition'daki Kritik Detay: `appProtocol` (INC-014)
Prometheus çalışırken şu hatayı alabilirsiniz:
```
dial tcp: lookup backend on 10.0.0.2:53: no such host
```

**Neden Olur?** Cloud Map'in HTTP discovery namespace'leri, VPC DNS sunucusuna (`10.0.0.2:53`) standart Route 53 A-kaydı yazmaz. DNS çözümlemesini konteynerin yanındaki Envoy sidecar proxy üstlenir. Envoy'un bu trafiği yakalayabilmesi için port mapping içinde açıkça **`appProtocol = "http"`** belirtilmelidir:

```hcl
portMappings = [
  {
    containerPort = 8080
    hostPort      = 8080
    protocol      = "tcp"
    name          = "spendsync-backend-8080-tcp"
    appProtocol   = "http" # ZORUNLU: Service Connect HTTP proxying için
  }
]
```

Ve servis seviyesinde Service Connect tanımlanır:
```hcl
service_connect_configuration {
  enabled   = true
  namespace = aws_service_discovery_http_namespace.internal.arn

  service {
    port_name      = "spendsync-backend-8080-tcp"
    discovery_name = "backend"
    client_alias {
      dns_name = "backend"
      port     = 8080
    }
  }
}
```

Artık VPC içindeki herhangi bir Service Connect üyesi servis, `http://backend:8080` yazdığı anda Envoy aracılığıyla backend'e yönlendirilir!

---

## 7. Adım 5: Kurumsal Ağ Güvenliği ve Defense-in-Depth

Grafana'yı public subnet'e koyup port 3000'i `0.0.0.0/0` şeklinde internete açmak çok büyük bir güvenlik açığıdır. Shodan, Shodan benzeri tarayıcılar ve botnet'ler panelinize dakikalar içinde brute-force saldırısı başlatır.

### 7.1. Güvenlik Seviyeleri ve Tercihimiz
Endüstride izleme panoları şu yöntemlerle korunur:
1. **Kurumsal VPN / Direct Connect:** En pahalı ve karmaşık yöntem.
2. **AWS SSM Port Forwarding:** Public IP yoktur, AWS CLI üzerinden yerel makineye tünel açılır (`aws ssm start-session ...`).
3. **Developer IP Whitelisting (`/32`):** Geliştirici veya ofis dış IP'si Security Group seviyesinde kilitlenir. İnternetin geri kalanı için port tamamen kapalıdır (SYN paketleri sessizce düşürülür).

Biz geliştirme ortamında hız ve ergonomi için **Developer IP Whitelisting** yaklaşımını tercih ettik:

```hcl
resource "aws_security_group_rule" "grafana_ingress_developer" {
  type              = "ingress"
  from_port         = 3000
  to_port           = 3000
  protocol          = "tcp"
  cidr_blocks       = [var.developer_ingress_cidr] # Örn: 85.105.xx.xx/32
  security_group_id = aws_security_group.grafana.id
}
```

### 7.2. Çok Katmanlı Güvenlik Matrisi (Security Matrix)

```
┌─────────────────────┬──────────────┬───────────────────────────────┬───────────────────────────────┐
│ Servis              │ Port         │ İzin Verilen Kaynak (Source)   │ Amacı                         │
├─────────────────────┼──────────────┼───────────────────────────────┼───────────────────────────────┤
│ ALB                 │ 80 / 443     │ 0.0.0.0/0 (Internet)          │ Dış kullanıcı trafiği         │
│ Backend             │ 8080         │ ALB SG + Prometheus SG        │ API + Metrik Kazıma           │
│ Prometheus          │ 9090         │ YALNIZCA Grafana SG           │ Dahili TSDB sorguları         │
│ Grafana             │ 3000         │ YALNIZCA Geliştirici IP (/32) │ Dashboard arayüzü             │
│ Amazon EFS          │ 2049 (NFS)   │ Prometheus SG + Grafana SG    │ Kalıcı disk okuma/yazma       │
│ PostgreSQL / Valkey │ 5432 / 6379  │ YALNIZCA Backend SG           │ Veritabanı ve önbellek        │
└─────────────────────┴──────────────┴───────────────────────────────┴───────────────────────────────┘
```

Grafana bile Backend'e doğrudan erişemez (INC-017). Tüm görselleştirme verisi strictly Prometheus TSDB üzerinden çekilir.

---

## 8. Adım 6: Yaşanan Canlı Ortam Tuzakları ve Çözümleri (Post-Mortem Çıkarımları)

Sıfırdan bir mimari ayağa kaldırırken en çok öğreten şeyler karşılaşılan krizlerdir. İşte bu kurulumda yaşadığımız ve çözdüğümüz 6 kritik olay:

### 1. Terraform Inline SG vs Standalone Rule Çakışması (INC-015)
- **Semptom:** `terraform apply` sırasında `InvalidPermission.Duplicate` hatası ve kuralların silinip tekrar oluşturulması döngüsü (state thrashing).
- **Kök Neden:** `aws_security_group` bloğunun içinde hem inline `ingress { ... }` hem de dışarıda bağımsız `aws_security_group_rule` kullanılması.
- **Ders:** Terraform projelerinde güvenlik grubu kurallarını daima bağımsız (`aws_security_group_rule`) kaynaklar olarak tanımlayın.

### 2. Cross-Module DAG Döngüsü (INC-016)
- **Semptom:** `Error: Cycle: module.ecs -> module.monitoring -> module.ecs`.
- **Kök Neden:** Monitoring modülünün namespace çıktısını ECS modülüne beslerken, monitoring'in de ECS cluster ID'sine bağımlı olması.
- **Ders:** Service Connect namespace'i çekirdek hesaplama katmanına aittir. `module.ecs` içinde tanımlanıp dışarı aktarılmalıdır (`vpc -> sg -> ecs -> monitoring`).

### 3. Cloud Map `ResourceInUse` Kilitlenmesi (INC-016)
- **Semptom:** Bir Service Connect namespace'ini silmek istediğinizde AWS'in izin vermemesi.
- **Kök Neden:** AWS Cloud Map, altında kayıtlı ECS servisleri varken namespace'in silinmesini engeller.
- **Ders:** Namespace yaşam döngüsünü ECS servislerinin yaşam döngüsünün üzerinde tutun.

---

## 9. Adım 7: Panolar ve Canlı Metrik Doğrulaması

Sistem ayağa kalktığında doğrulama aşamaları:

### 9.1. Prometheus Target Durumu
Prometheus üzerinde `backend:8080/actuator/prometheus` hedefinin sağlık durumu sorgulanır:
- `State:` **UP**
- `Scrape Duration:` **~180ms - 220ms**
- `Last Error:` `""` (Hata yok)

### 9.2. Canlı Grafana Panosu
Geliştirici IP'miz üzerinden Grafana'ya girdiğimizde (`http://<GRAFANA_IP>:3000`) karşılaştığımız SpendSync Platform & JVM Overview panosu:
- **JVM Heap Bellek:** Eden (~62 MB), Old Gen (~89 MB), Survivor (~10 MB) anlık dağılımı.
- **Garbage Collection:** G1GC duraklama süreleri ve sıklığı.
- **HikariCP Pool:** Aktif ve boşta bekleyen veritabanı bağlantı sayıları.
- **HTTP Latency:** p50, p95 ve p99 milisaniye bazında yanıt süreleri.

Tüm metrikler tek bir sayfada, sıfır veri kaybıyla ve gerçek zamanlı olarak akmaktadır.

---

## 10. FinOps Dokunuşu: Night Sleep Mode ile Maliyeti Sıfıra Yaklaştırmak

Geliştirme (dev) ortamları gece ve hafta sonları genellikle boştadır. AWS Fargate'te çalışan görevleri gece kapatıp sabah tekrar açarak compute maliyetini **%65 daha fazla düşürebiliriz**.

EFS kullandığımız için konteynerleri kapattığımızda hiçbir metrik veya pano ayarı kaybolmaz!

```bash
#!/usr/bin/env bash
# infra/scripts/night-sleep.sh
echo "Gece modu devrede: ECS servisleri uyku moduna alınıyor..."
aws ecs update-service --cluster spendsync-dev-cluster --service spendsync-dev-backend --desired-count 0
aws ecs update-service --cluster spendsync-dev-cluster --service spendsync-dev-prometheus --desired-count 0
aws ecs update-service --cluster spendsync-dev-cluster --service spendsync-dev-grafana --desired-count 0
echo "Tüm servisler kapatıldı. Aylık tasarruf maksimize edildi!"
```

Sabah mesai başladığında:
```bash
#!/usr/bin/env bash
# infra/scripts/morning-wake.sh
echo "Günaydın! Servisler uyandırılıyor..."
aws ecs update-service --cluster spendsync-dev-cluster --service spendsync-dev-backend --desired-count 1
aws ecs update-service --cluster spendsync-dev-cluster --service spendsync-dev-prometheus --desired-count 1
aws ecs update-service --cluster spendsync-dev-cluster --service spendsync-dev-grafana --desired-count 1
```

---

## 11. Sonuç ve Üretim Ortamı Kontrol Listesi (Checklist)

ECS üzerinde kendi observability altyapınızı kurmak, hem derinlemesine bulut mühendisliği yetkinliği kazandırır hem de şirketinizi gereksiz SaaS faturalarından kurtarır.

### Kendi Ortamınız İçin Kontrol Listesi:
- [x] Spring Boot `micrometer-registry-prometheus` bağımlılığı eklendi mi?
- [x] Spring Security filtre zincirinde `/actuator/prometheus` izni verildi mi?
- [x] EFS Access Point'lerde POSIX UID/GID (Prometheus 65534, Grafana 472) doğru ayarlandı mı?
- [x] Prometheus ECS servisinde `deployment_maximum_percent = 100` yapıldı mı? (TSDB lock koruması)
- [x] ECS Service Connect için task definition port mapping'inde `appProtocol = "http"` tanımlandı mı?
- [x] Grafana port 3000 girişi sadece yetkili IP adresiyle (`/32`) sınırlandırıldı mı?
- [x] Prometheus ve Grafana konfigürasyonları Docker imajına gömüldü mü (Config-as-Code)?

Happy monitoring! 🚀
