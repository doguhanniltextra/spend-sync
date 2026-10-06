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
