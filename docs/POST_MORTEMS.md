# Engineering Post-Mortems and Incident Reports

This document records technical incidents encountered during infrastructure deployment, containerization, continuous integration, and application execution for the SpendSync platform. Each report details symptoms, root causes, corrective actions, and preventative controls.

---

## Index of Incidents

1. [INC-001: AWS Organization Service Control Policy Blocks OIDC Identity Provider Creation](#inc-001-aws-organization-service-control-policy-blocks-oidc-identity-provider-creation)
2. [INC-002: Browser Mixed Content Policy and CORS Failure on CloudFront-to-ALB Communication](#inc-002-browser-mixed-content-policy-and-cors-failure-on-cloudfront-to-alb-communication)
3. [INC-003: Client-Side Single Page Application Route Resolution Failing with HTTP 403/404 on S3](#inc-003-client-side-single-page-application-route-resolution-failing-with-http-403404-on-s3)
4. [INC-004: Successful HTTP 200 Login Triggers Erroneous Authentication Failure Display](#inc-004-successful-http-200-login-triggers-erroneous-authentication-failure-display)
5. [INC-005: Continuous Integration Pipeline Inadvertently Targets Development Infrastructure on Pull Request](#inc-005-continuous-integration-pipeline-inadvertently-targets-development-infrastructure-on-pull-request)
6. [INC-006: PostgreSQL Schema Constraint Violation on Refresh Token Nullability During Authentication](#inc-006-postgresql-schema-constraint-violation-on-refresh-token-nullability-during-authentication)
7. [INC-007: Missing Linux Platform Binary for Rollup Compiler in CI/CD Build Environment](#inc-007-missing-linux-platform-binary-for-rollup-compiler-in-cicd-build-environment)
8. [INC-008: JVM Memory Allocation Overrun Leading to Linux OOMKilled Termination in Fargate](#inc-008-jvm-memory-allocation-overrun-leading-to-linux-oomkilled-termination-in-fargate)
9. [INC-009: Terraform S3 Remote State Bootstrap Dependency Cycle](#inc-009-terraform-s3-remote-state-bootstrap-dependency-cycle)
10. [INC-010: Application Load Balancer Health Check Failure Due to Spring Security Filter Evaluation](#inc-010-application-load-balancer-health-check-failure-due-to-spring-security-filter-evaluation)
11. [INC-011: Valkey In-Transit TLS Configuration Mismatch Triggering Rate Limiter Connection Timeouts](#inc-011-valkey-in-transit-tls-configuration-mismatch-triggering-rate-limiter-connection-timeouts)
12. [INC-012: Prometheus TSDB Single-Writer File Lock Contention on EFS in ECS Fargate](#inc-012-prometheus-tsdb-single-writer-file-lock-contention-on-efs-in-ecs-fargate)
13. [INC-013: Spring Boot Actuator HTTP 404 Due to Missing Micrometer Prometheus Dependency and Security Permit](#inc-013-spring-boot-actuator-http-404-due-to-missing-micrometer-prometheus-dependency-and-security-permit)
14. [INC-014: ECS Service Connect DNS Resolution Failure Due to Missing appProtocol and Alias Drift](#inc-014-ecs-service-connect-dns-resolution-failure-due-to-missing-appprotocol-and-alias-drift)
15. [INC-015: Terraform Inline Security Group Ingress Conflict with Standalone Security Group Rules](#inc-015-terraform-inline-security-group-ingress-conflict-with-standalone-security-group-rules)
16. [INC-016: Terraform Cross-Module DAG Dependency Cycle and Cloud Map Namespace Lifecycle Deadlock](#inc-016-terraform-cross-module-dag-dependency-cycle-and-cloud-map-namespace-lifecycle-deadlock)
17. [INC-017: Defense-in-Depth Security Group Network Isolation Triggering Grafana Upstream Timeout](#inc-017-defense-in-depth-security-group-network-isolation-triggering-grafana-upstream-timeout)

---

## INC-001: AWS Organization Service Control Policy Blocks OIDC Identity Provider Creation

- **Component:** IAM / CI/CD Bootstrap
- **Severity:** High (Blocked CI/CD deployment authentication)
- **Status:** Resolved

### Description
During bootstrap configuration in `infra/bootstrap/`, the initial plan called for configuring GitHub Actions OpenID Connect (OIDC) identity federation via `aws_iam_openid_connect_provider` to eliminate static IAM credentials. Applying the Terraform configuration failed with an AWS AccessDenied error.

### Root Cause
The AWS account is enrolled within an AWS Organizations hierarchy governed by a Service Control Policy (SCP). The policy contains an explicit deny on `iam:CreateOpenIDConnectProvider`, overriding local administrator permissions.

### Resolution
Authentication was refactored to use a dedicated, least-privilege IAM service user (`spendsync-github-deployer`). Policies attached to this user were constrained to the exact resources managed by the deployment pipeline:
- ECR repository authorization and push rights on `arn:aws:ecr:eu-north-1:<AWS_ACCOUNT_ID>:repository/spendsync-backend`.
- S3 read/write permissions scoped to state and asset buckets.
- ECS service update and task definition registration permissions.
- CloudFront cache invalidation creation permissions.

Access keys were generated and stored as encrypted GitHub repository secrets (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`).

---

## INC-002: Browser Mixed Content Policy and CORS Failure on CloudFront-to-ALB Communication

- **Component:** Networking / Frontend API Client
- **Severity:** High (Complete failure of API calls in deployed frontend)
- **Status:** Resolved

### Description
Following the deployment of static frontend assets to CloudFront (`https://d111111abcdef8.cloudfront.net`), user login requests failed immediately in modern browsers. DevTools reported:
1. `Mixed Content: The page at 'https://...' was loaded over HTTPS, but requested an insecure XMLHttpRequest endpoint 'http://spendsync-dev-alb-...'. This request has been blocked.`
2. `Access to XMLHttpRequest has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.`

### Root Cause
The frontend was served via HTTPS through CloudFront, while the ALB public listener operated over unencrypted HTTP (port 80). Web browsers enforce strict Mixed Content policies preventing active network requests from secure contexts to insecure endpoints. Furthermore, cross-origin requests between CloudFront and ALB required CORS headers that were not configured for the CloudFront domain.

### Resolution
Architected a path-based reverse proxy on the CloudFront distribution:
- Created a secondary origin pointing directly to the ALB DNS name.
- Configured a cache behavior matching `/api/*` routed to the ALB origin.
- Assigned `Managed-CachingDisabled` and `Managed-AllViewerExceptHostHeader` policies.
- Updated frontend production configuration (`.env.production`) to use relative paths (`VITE_API_BASE_URL=""`).
- Result: Static assets and API endpoints are served from the same HTTPS domain, eliminating both Mixed Content and cross-origin restrictions.

---

## INC-003: Client-Side Single Page Application Route Resolution Failing with HTTP 403/404 on S3

- **Component:** Frontend Hosting / Amazon S3 / CloudFront
- **Severity:** Medium (Users unable to refresh or directly access application URLs)
- **Status:** Resolved

### Description
While initial navigation through the landing page worked, directly accessing or refreshing subpaths such as `/login`, `/dashboard`, or `/requisitions` returned an XML error from Amazon S3 indicating `AccessDenied` (HTTP 403) or `NoSuchKey` (HTTP 404).

### Root Cause
Amazon S3 is an object store where keys represent exact file paths. Client-side routing libraries (React Router v6) handle navigation virtually in browser memory. When a browser requests `https://.../dashboard`, S3 attempts to locate an object named `dashboard` or `dashboard/index.html`. Finding none, and because public listing is blocked, S3 returns HTTP 403 or 404.

### Resolution
Configured CloudFront Custom Error Responses in `infra/modules/frontend/main.tf`:
```hcl
custom_error_response {
  error_code            = 403
  response_code         = 200
  response_page_path    = "/index.html"
  error_caching_min_ttl = 0
}

custom_error_response {
  error_code            = 404
  response_code         = 200
  response_page_path    = "/index.html"
  error_caching_min_ttl = 0
}
```
All unresolved object requests are rewritten to `/index.html` with HTTP 200, allowing React Router to inspect the browser path and render the appropriate view.

---

## INC-004: Successful HTTP 200 Login Triggers Erroneous Authentication Failure Display

- **Component:** Frontend State Management (`LoginPage.tsx`, `useAuthStore.ts`)
- **Severity:** High (Users blocked from accessing application despite valid credentials)
- **Status:** Resolved

### Description
Users attempting to sign in with valid credentials (`cfo@spendsync.com`) received a red UI banner stating `"Invalid email or password."`. Inspection of browser network traffic confirmed that `POST /api/v1/auth/login` succeeded with HTTP 200 OK and returned a valid JSON payload containing access and refresh tokens.

### Root Cause
Analysis of `LoginPage.tsx` and `useAuthStore.ts` identified two compound defects:
1. **Monolithic Error Handling:** In `LoginPage.tsx`, the API invocation (`authApi.login`), the store synchronization (`setAuth`), and the client-side navigation (`navigate(ROUTES.dashboard)`) were enclosed in a single `try / catch` block. Any JavaScript runtime exception occurring after the network call was caught by the same handler.
2. **Unsafe Collection Parsing:** In `useAuthStore.ts`, the role normalization logic executed:
   ```typescript
   roles: Array.isArray(response.roles)
     ? response.roles
     : Array.from(response.roles as unknown as Set<string>)
   ```
   When `response.roles` was undefined or formatted differently, `Array.from(undefined)` threw an uncaught `TypeError: undefined is not iterable`.
3. Because the resulting error object lacked a `response` property (not an Axios error), the catch handler defaulted to setting `MESSAGES.auth.loginError` (`"Invalid email or password."`), misrepresenting an internal runtime error as an invalid credential rejection.

### Resolution
1. **Isolated Asynchronous Actions:** Separated the network request from state hydration and navigation in `LoginPage.tsx`. Only HTTP 4xx/5xx responses populate the server error message.
2. **Defensive Role Hydration:** Refactored `useAuthStore.ts` to validate iterable properties before calling `Array.from()`, guaranteeing `user.roles` defaults to an empty array.
3. **Navigation Fallback:** Wrapped post-authentication navigation in a defensive block with a secondary fallback to `window.location.href = ROUTES.dashboard`.
4. **Active Session Detection:** Added an authentication listener redirecting authenticated sessions to the dashboard automatically.

---

## INC-005: Continuous Integration Pipeline Inadvertently Targets Development Infrastructure on Pull Request

- **Component:** GitHub Actions Workflow (`infra.yml`)
- **Severity:** Medium (Risk of unintended configuration changes in dev environment)
- **Status:** Resolved

### Description
Pushing changes containing the new `infra/envs/prod` directory triggered the `Infrastructure Automation (Terraform)` workflow on GitHub Actions. The job executed against `infra/envs/dev` and attempted to plan development infrastructure changes without explicit operator intent.

### Root Cause
`infra.yml` contained:
```yaml
on:
  push:
    branches: [main]
    paths:
      - 'infra/**'
```
The execution defaults were hardcoded to `working-directory: infra/envs/dev`. Consequently, any push touching files under `infra/` (including production manifests) automatically triggered Terraform operations against the development state.

### Resolution
1. Removed the `push: branches: [main]` trigger from `infra.yml`.
2. Parameterized the `workflow_dispatch` trigger to require explicit environment selection:
   ```yaml
   inputs:
     environment:
       type: choice
       options: [dev, prod]
       default: dev
     apply_changes:
       type: boolean
       default: false
   ```
3. Dynamically set the working directory to `infra/envs/${{ inputs.environment }}` and the variable file to `${{ inputs.environment }}.tfvars`.

---

## INC-006: PostgreSQL Schema Constraint Violation on Refresh Token Nullability During Authentication

- **Component:** Database Schema / Hibernate Mapping
- **Severity:** High (Authentication service threw HTTP 500 when saving tokens)
- **Status:** Resolved

### Description
Authentication requests failed with HTTP 500 Internal Server Error. Backend application logs showed a PostgreSQL integrity violation:
`ERROR: null value in column "token" of relation "refresh_tokens" violates not-null constraint`.

### Root Cause
A refactoring in the backend domain layer transitioned the `RefreshToken` entity from storing raw token strings in the `token` column to persisting cryptographic hashes in `token_hash`. While the entity mapping left `token` unpopulated, database migration scripts prior to `V1_16` maintained a strict `NOT NULL` constraint on the legacy column.

### Resolution
Authored and applied Flyway migration `V1_16__make_refresh_tokens_token_nullable.sql`:
```sql
ALTER TABLE refresh_tokens ALTER COLUMN token DROP NOT NULL;
```
The change synchronized the physical schema with the entity persistence model.

---

## INC-007: Missing Linux Platform Binary for Rollup Compiler in CI/CD Build Environment

- **Component:** Frontend Build System / Vite / Rollup
- **Severity:** High (Frontend deployment pipeline build failure)
- **Status:** Resolved

### Description
Executing `npm run build` within the self-hosted Linux runner environment threw a module resolution exception:
`Cannot find module @rollup/rollup-linux-x64-gnu`. The Vite build process halted with exit code 1.

### Root Cause
Rollup 4 utilizes optional platform-specific native binaries for performance. When `package.json` was generated or dependencies were installed in non-Linux environments, optional dependencies targeting Linux x86_64 were excluded from the lockfile or omitted by clean install scripts.

### Resolution
Explicitly installed and recorded the Linux binary in `frontend/package.json`:
```bash
npm install --save-optional @rollup/rollup-linux-x64-gnu
```
Ensured `npm ci` in CI/CD correctly links the native binding during Linux builds.

---

## INC-008: JVM Memory Allocation Overrun Leading to Linux OOMKilled Termination in Fargate

- **Component:** Container Runtime / JVM Configuration / ECS Fargate
- **Severity:** High (Intermittent container crash with exit code 137)
- **Status:** Resolved

### Description
During stress testing, the backend container running in ECS Fargate Spot terminated unexpectedly. The task stopped reason indicated:
`Essential container in task exited with code 137`.

### Root Cause
Exit code 137 indicates the container received `SIGKILL` (signal 9) from the Linux kernel Out-Of-Memory (OOM) killer. The task definition allocated 1 GB (1024 MB) of total memory. By default, older Java runtime heuristics or unconstrained metaspace/direct memory allocations can exceed container cgroup thresholds, triggering an immediate process kill.

### Resolution
Updated `JAVA_OPTS` in `backend/Dockerfile` to enforce strict cgroup memory quotas:
```bash
-XX:+UseG1GC \
-XX:MaxRAMPercentage=75.0 \
-XX:InitialRAMPercentage=50.0 \
-XX:+ExitOnOutOfMemoryError \
-Djava.security.egd=file:/dev/./urandom
```
Limiting the maximum heap to 75% of container RAM guarantees sufficient headroom (~256 MB) for thread stacks, metaspace, and off-heap allocations, eliminating kernel termination.

---

## INC-009: Terraform S3 Remote State Bootstrap Dependency Cycle

- **Component:** Infrastructure as Code / Terraform State
- **Severity:** Medium (Initial environment provisioning blocker)
- **Status:** Resolved

### Description
Configuring the Terraform S3 backend in `infra/envs/dev/providers.tf` required an existing S3 bucket (`spendsync-tf-state-...`). However, defining this bucket in the same Terraform configuration caused a circular dependency: Terraform cannot initialize remote state storage before the state storage resource is provisioned.

### Root Cause
Standard Terraform bootstrap lifecycle requires separated phases for foundational storage versus application infrastructure.

### Resolution
Implemented a two-tier provisioning workflow:
1. Authored `infra/bootstrap/` to manage the state bucket, ECR repository, and deployment user using local state.
2. Executed `terraform apply` locally in `infra/bootstrap/`.
3. Added `backend.tf` to `infra/bootstrap/` and ran `terraform init -migrate-state` to migrate local state into S3.
4. Leveraged Terraform 1.11 native S3 lockfiles (`use_lockfile = true`), eliminating the need for a separate Amazon DynamoDB table.

---

## INC-010: Application Load Balancer Health Check Failure Due to Spring Security Filter Evaluation

- **Component:** Spring Security / ALB Target Group
- **Severity:** Critical (ECS service stuck in continuous task restart loop)
- **Status:** Resolved

### Description
Following initial deployment to ECS Fargate, the task started successfully but was deregistered by the ALB after approximately 3 minutes. The service event log reported:
`Task failed ELB health checks in target-group spendsync-dev-tg`. ECS terminated the container and launched a replacement, creating a perpetual deployment failure loop.

### Root Cause
The Application Load Balancer target group was configured to evaluate HTTP responses on path `/actuator/health`. Spring Security intercepted this path and, in the absence of an authorization header, returned HTTP 401 Unauthorized. The target group matcher expected HTTP 200, classifying the container as unhealthy.

### Resolution
Modified `SecurityConfig.java` to explicitly permit unauthenticated access to health monitoring endpoints:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/api/v1/auth/**").permitAll()
    .anyRequest().authenticated()
)
```
The ALB health probe consistently received HTTP 200 responses, maintaining container registration.

---

## INC-011: Valkey In-Transit TLS Configuration Mismatch Triggering Rate Limiter Connection Timeouts

- **Component:** Redis / ElastiCache Valkey / Spring Data Redis
- **Severity:** Critical (All HTTP endpoints returned HTTP 500 due to rate limiter timeout)
- **Status:** Resolved

### Description
Upon deploying to AWS, all incoming HTTP requests to the backend failed with HTTP 500 Internal Server Error. Logs revealed:
`org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis`. The `RedisRateLimiter` filter timed out while evaluating token bucket operations on each incoming request.

### Root Cause
Local development executed against a standalone Redis container without SSL encryption (`REDIS_SSL_ENABLED=false`). In contrast, Amazon ElastiCache Serverless Valkey mandates in-transit TLS encryption on port 6379. The Spring Boot application attempted plain TCP connections against a TLS-enforced port, causing SSL handshake failures and connection timeouts.

### Resolution
1. Set environment variable `REDIS_SSL_ENABLED=true` within the ECS task definition in `infra/modules/ecs/main.tf`.
2. Verified Spring Boot's `LettuceClientConfiguration` conditionally enables SSL when `spring.data.redis.ssl.enabled=true`.
3. Following container restart, Lettuce established encrypted TLS channels to the Valkey cluster, restoring rate limiter functionality.

---

## INC-012: Prometheus TSDB Single-Writer File Lock Contention on EFS in ECS Fargate

- **Component:** Prometheus TSDB / Amazon EFS / ECS Task Lifecycle
- **Severity:** High (CrashLoopBackOff during service updates, Prometheus service unavailable)
- **Status:** Resolved

### Description
During ECS service updates or configuration reloads, new Prometheus tasks entering the `PENDING` -> `RUNNING` transition repeatedly terminated with exit code 1 or 2. CloudWatch logs displayed the fatal error:
```
tsdb: open /prometheus/lock: resource temporarily unavailable
FAILED: Opening storage failed
```
The newly launched container crashed immediately, causing ECS to reschedule repeatedly in a CrashLoopBackOff loop.

### Root Cause
Prometheus employs an embedded TSDB (Time Series Database) with strict single-writer semantics enforced via an exclusive operating system file lock (`/prometheus/lock` utilizing `flock` / `fcntl`). The storage directory is mounted to persistent Amazon EFS via an EFS Access Point (`/prometheus`).

Under default ECS Fargate rolling deployment configurations:
`maximum_percent = 200`, `minimum_healthy_percent = 100`
ECS starts the new task *before* stopping the existing task. Because both tasks mount the identical EFS directory simultaneously, the existing task held the open file lock descriptor on `/prometheus/lock`. The new task attempted to acquire the exclusive lock, received `EWOULDBLOCK` / `EAGAIN`, and exited fatally.

### Resolution
1. **Deployment Strategy Reconfiguration:** Reconfigured the Prometheus ECS service deployment parameters to enforce single-instance stateful deployment:
   ```hcl
   deployment_maximum_percent         = 100
   deployment_minimum_healthy_percent = 0
   ```
2. **Immediate Remediation:** Manually terminated the predecessor Prometheus task (`aws ecs stop-task`) to release the NFS/EFS lock descriptor before launching the replacement task.
3. **Data Integrity Verification:** Verified that the TSDB WAL (Write-Ahead Log) recovered cleanly upon subsequent single-instance boot without data corruption.

### Preventative Controls
- Enforce `deployment_maximum_percent = 100` and `deployment_minimum_healthy_percent = 0` for all stateful workloads backed by shared NFS/EFS persistent volumes requiring exclusive file locks.
- For high-availability multi-replica metrics collection in future phases, adopt Thanos sidecars or Cortex/Mimir architecture rather than shared EFS volumes.

---

## INC-013: Spring Boot Actuator HTTP 404 Due to Missing Micrometer Prometheus Dependency and Security Permit

- **Component:** Spring Boot 3.3.5 / Micrometer Metrics / Spring Security 6
- **Severity:** High (Complete absence of application metrics, Prometheus scrape target failing)
- **Status:** Resolved

### Description
Following the configuration of Prometheus to scrape the backend at `/actuator/prometheus`, Prometheus reported target status `DOWN` with HTTP 404 Not Found. While standard health checks (`/actuator/health`) returned HTTP 200 OK, the Prometheus exposition endpoint did not exist on the Spring Boot server.

### Root Cause
1. **Missing Runtime Dependency:** While `spring-boot-starter-actuator` was declared in `pom.xml`, Spring Boot Actuator does not automatically generate Prometheus-formatted text metrics (`text/plain; version=0.0.4`) without the `io.micrometer:micrometer-registry-prometheus` adapter present on the runtime classpath.
2. **Security Filter Evaluation:** Spring Security's filter chain evaluated `/actuator/prometheus` under default authenticated rules (`.anyRequest().authenticated()`), returning HTTP 401/403 to unauthenticated internal probes even if the endpoint had been active.

### Resolution
1. **Dependency Injection:** Packaged `micrometer-registry-prometheus-1.13.0.jar` into the backend runtime classpath and updated the application build definition.
2. **Exposition Configuration:** Explicitly exposed Prometheus in `application.yml`:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,prometheus
     prometheus:
       metrics:
         export:
           enabled: true
   ```
3. **Security Authorization Whitelist:** Updated `SecurityConfig.java` to explicitly permit internal monitoring traffic:
   ```java
   .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
   ```
4. Rebuilt and pushed the backend Docker image, resulting in immediate HTTP 200 responses with JVM, HikariCP, and HTTP request metrics.

---

## INC-014: ECS Service Connect DNS Resolution Failure Due to Missing appProtocol and Alias Drift

- **Component:** ECS Service Connect / AWS Cloud Map / Envoy Sidecar / CoreDNS
- **Severity:** Critical (Total internal service discovery failure; Prometheus unable to resolve backend endpoint)
- **Status:** Resolved

### Description
Prometheus reported target error:
```
Get "http://backend:8080/actuator/prometheus": dial tcp: lookup backend on 10.0.0.2:53: no such host
```
The scrape engine was completely unable to resolve `backend` or `backend:8080` via internal DNS, resulting in zero ingested metrics.

### Root Cause
1. **VPC DNS vs. Service Connect Architecture:** Cloud Map namespaces created for ECS Service Connect with `discovery_type = "HTTP"` do NOT register standard Route 53 A-records in Amazon VPC Route 53 resolver (`10.0.0.2:53`). Standard DNS lookups directed to VPC DNS inevitably return `NXDOMAIN`.
2. **Missing `appProtocol` Declaration:** In AWS ECS Service Connect, traffic interception and local proxy routing require `appProtocol = "http"` (or `"tcp"`) declared explicitly within the task definition `portMappings`. Without `appProtocol`, ECS does not configure Envoy to intercept and forward traffic for the declared client aliases.
3. **Target & Alias Naming Drift:** The Service Connect client alias in ECS was initially configured with port or name variations that did not match the Prometheus scrape configuration target (`backend:8080`).

### Resolution
1. **Task Definition Port Mapping:** Updated the backend task definition in `infra/modules/ecs/main.tf` to explicitly declare `appProtocol = "http"`:
   ```hcl
   portMappings = [{
     containerPort = 8080
     hostPort      = 8080
     protocol      = "tcp"
     name          = "spendsync-backend-8080-tcp"
     appProtocol   = "http"
   }]
   ```
2. **Service Connect Alias Alignment:** Unified the Service Connect client alias on the backend service:
   ```hcl
   client_alias {
     dns_name = "backend"
     port     = 8080
   }
   ```
3. **Prometheus Scrape Configuration Alignment:** Configured `prometheus.yml` scrape target exactly to `backend:8080`:
   ```yaml
   static_configs:
     - targets: ['backend:8080']
   ```
4. Re-deployed both ECS services with Service Connect active. Prometheus resolved `backend:8080` via Envoy sidecar interception with scrape duration < 250ms and state `UP`.

---

## INC-015: Terraform Inline Security Group Ingress Conflict with Standalone Security Group Rules

- **Component:** Terraform AWS Provider / Security Groups State Management
- **Severity:** Medium (Pipeline deployment blocker, Terraform state collision)
- **Status:** Resolved

### Description
Executing `terraform apply` during the addition of monitoring ingress rules failed with:
```
Error: creating Security Group Rule: InvalidPermission.Duplicate: the specified rule already exists
```
Subsequent runs attempted to delete the Prometheus rule on every cycle and recreate it, thrashing Terraform state.

### Root Cause
In Terraform, defining ingress rules inline within the `aws_security_group` resource (`ingress { ... }`) while simultaneously managing rules via standalone `aws_security_group_rule` resources creates a resource conflict. Terraform's AWS provider considers inline rules and standalone rules authoritative for the entire security group ingress rule set, causing them to overwrite and conflict with each other during state reconciliation.

### Resolution
1. Refactored `infra/modules/security_groups/main.tf` to eliminate inline ingress blocks on the ECS security group.
2. Standardized all ingress rules across the module as standalone `aws_security_group_rule` resources (e.g., `aws_security_group_rule.alb_to_ecs_ingress`, `aws_security_group_rule.prometheus_to_backend_ingress`).
3. Imported pre-existing unmanaged AWS rules into the Terraform state:
   ```bash
   terraform import module.security_groups.aws_security_group_rule.alb_to_ecs_ingress sgr-xxxxxxxxxxxxxxxxx
   ```
4. State converged to zero unintended changes with stable drift-free plans.

---

## INC-016: Terraform Cross-Module DAG Dependency Cycle and Cloud Map Namespace Lifecycle Deadlock

- **Component:** Terraform DAG Compiler / AWS Cloud Map (`aws_service_discovery_http_namespace`)
- **Severity:** High (Deployment deadlock, circular dependency preventing infrastructure apply)
- **Status:** Resolved

### Description
Attempting to pass the Service Connect namespace created in `module.monitoring` into `module.ecs` triggered a fatal Terraform graph cycle:
```
Error: Cycle: module.ecs -> module.monitoring -> module.ecs
```
Attempting to destroy and recreate the namespace failed on AWS with:
```
ResourceInUse: Namespace ns-xxxxxxxxxxxx contains one or more registered services
```

### Root Cause
1. **Circular Dependency:** `module.monitoring` depended on `module.ecs.cluster_id` to attach monitoring ECS services, while `module.ecs` depended on `module.monitoring.service_connect_namespace_id` to attach Service Connect configurations to the backend service. This formed a cyclic dependency graph in Terraform's DAG resolution.
2. **Cloud Map Resource Lock:** AWS Cloud Map forbids deleting an HTTP or Private DNS namespace while active service discovery services remain registered inside it.

### Resolution
1. **Module Hierarchy Inversion:** Relocated the `aws_service_discovery_http_namespace` declaration into `infra/modules/ecs/service_connect.tf`. Because the core ECS cluster owns the service discovery mesh, the namespace belongs architecturally to the foundational compute layer.
2. **Linear DAG Flow:** Restructured the module dependency graph into a strict single-direction pipeline:
   `module.vpc` -> `module.security_groups` -> `module.ecs` (exports namespace) -> `module.monitoring`.
3. Applied cleanly without cycles or Cloud Map orphaned resource locks.

---

## INC-017: Defense-in-Depth Security Group Network Isolation Triggering Grafana Upstream Timeout

- **Component:** AWS Security Groups / Grafana Data Sources / Defense-in-Depth Architecture
- **Severity:** Low / Informational (Design validation; prevented unauthorized lateral service access)
- **Status:** Resolved

### Description
When configuring initial Grafana dashboards, attempting to configure a direct HTTP connection from Grafana to `http://backend:8080/actuator/prometheus` resulted in an immediate timeout:
```
504 Gateway Timeout / upstream request timeout
```

### Root Cause
Our security group architecture enforces strict zero-trust network segregation:
- **Backend SG Ingress:** Permits port 8080 strictly from `module.security_groups.alb_security_group_id` and `module.security_groups.prometheus_security_group_id`.
- **Grafana SG:** Possesses no ingress rule into the backend security group.
This isolation is by design: dashboard consumers should never scrape or query backend application servers directly, as this bypasses time-series buffering, introduces performance degradation on application containers, and violates least-privilege networking.

### Resolution
1. Confirmed architectural policy: Grafana must only communicate with Prometheus on port 9090 (`http://prometheus:9090`).
2. Configured Prometheus as the sole authoritative datasource in Grafana (`datasources.yml`), keeping the backend network surface completely shielded from external visualization components.
3. Verified that all application, JVM, and infrastructure dashboards render instantaneously from Prometheus TSDB without touching the backend container network boundary directly.
