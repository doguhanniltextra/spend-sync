# SpendSync Operations Runbook

This document defines standard operating procedures for deploying, verifying, troubleshooting, and rolling back services within the SpendSync platform on Amazon Web Services.

---

## 1. Prerequisites and Access Management

### 1.1. Required Local Tooling
- AWS CLI v2 (`>= 2.15.0`)
- Terraform (`>= 1.11.0`)
- Docker Engine (`>= 24.0.0`)
- Node.js (`>= 20.0.0`) and npm
- Java JDK 21 and Apache Maven

### 1.2. AWS CLI Configuration
Operators must configure the dedicated AWS CLI profile `spendsync`:
```bash
aws configure --profile spendsync
# AWS Access Key ID: [Stored securely in password manager]
# AWS Secret Access Key: [Stored securely in password manager]
# Default region name: eu-north-1
# Default output format: json
```

Verify authentication and permissions:
```bash
aws sts get-caller-identity --profile spendsync
```
Expected output:
```json
{
    "UserId": "AIDAX...",
    "Account": "<AWS_ACCOUNT_ID>",
    "Arn": "arn:aws:iam::<AWS_ACCOUNT_ID>:user/ci-cd/spendsync-github-deployer"
}
```

---

## 2. Standard Deployment Procedures

Continuous delivery workflows are defined in `.github/workflows/`. In compliance with change management policies, production and development AWS deployments do not trigger automatically on code push.

### 2.1. Backend Service Deployment
1. Navigate to the GitHub Actions console: **Actions > Backend Build, Package & Deploy**.
2. Click **Run workflow**.
3. Select the target branch (`main`) and choose execution options:
   - `skip_tests`: `false` (default)
   - `smoke_tests`: `true` (runs automated HTTP validation post-deployment)
4. Monitor pipeline stages:
   - Compile and verify with Maven.
   - Build multi-stage Docker container image.
   - Push image tagged with `sha-<commit>` and `latest` to Amazon ECR.
   - Register updated ECS Task Definition revision.
   - Initiate rolling service update on `spendsync-dev-backend`.
   - Execute post-deploy smoke tests against public endpoints.

### 2.2. Frontend Application Deployment
1. Navigate to **Actions > Frontend Build & Deploy to S3 / CloudFront**.
2. Click **Run workflow** against branch `main`.
3. The workflow executes:
   - `npm ci` clean dependency installation.
   - `npm run build` targeting production environment (`.env.production`).
   - S3 synchronization:
     ```bash
     aws s3 sync frontend/dist s3://spendsync-dev-frontend-<AWS_ACCOUNT_ID> --delete --profile spendsync
     ```
   - Global CDN cache invalidation:
     ```bash
     aws cloudfront create-invalidation --distribution-id <DISTRIBUTION_ID> --paths "/*" --profile spendsync
     ```

### 2.3. Infrastructure Provisioning via Terraform
1. Navigate to **Actions > Infrastructure Automation (Terraform)**.
2. Select target environment (`dev` or `prod`).
3. Set `apply_changes: true` only when ready to execute; leave unchecked to generate an informational plan.

To execute locally using the CLI:
```bash
cd infra/envs/dev
terraform init
terraform plan -var-file=dev.tfvars -out=dev.tfplan
terraform apply dev.tfplan
```

---

## 3. Post-Deployment Verification

### 3.1. Automated Verification
The deployment pipeline automatically validates the following assertions:
- `GET /actuator/health` returns `{"status":"UP"}` with HTTP 200.
- `POST /api/v1/auth/login` accepts seed credentials and returns a valid JWT structure.
- `GET /api/v1/budget/summary` returns budget utilization records.
- `GET /api/v1/intelligence/pulse` responds within 2000 ms.

### 3.2. Manual Verification Commands
Execute the following verification commands from an authorized terminal:

```bash
# Check Backend Health Endpoint via CloudFront Reverse Proxy
curl -s -i https://d111111abcdef8.cloudfront.net/actuator/health | grep "HTTP/2 200"

# Check Backend Health Directly via ALB
curl -s -i http://spendsync-dev-alb-<ALB_ID>.<REGION>.elb.amazonaws.com/actuator/health | grep "HTTP/1.1 200"

# Verify Frontend Index Page Availability
curl -s -i https://d111111abcdef8.cloudfront.net/index.html | grep "HTTP/2 200"

# Execute End-to-End Authentication
curl -s -X POST https://d111111abcdef8.cloudfront.net/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cfo@spendsync.com","password":"Password123!"}' | grep "accessToken"
```

---

## 4. Rollback Procedures

### 4.1. Backend Rollback (ECS Task Definition)
If a newly deployed container exhibits critical errors or performance regressions:

1. Identify the previous stable task definition revision:
```bash
aws ecs describe-services \
  --cluster spendsync-dev-cluster \
  --services spendsync-dev-backend \
  --query "services[0].taskDefinition" \
  --output text \
  --profile spendsync
```
2. Roll back to the prior revision (e.g., revision 3):
```bash
aws ecs update-service \
  --cluster spendsync-dev-cluster \
  --service spendsync-dev-backend \
  --task-definition spendsync-dev-backend:3 \
  --force-new-deployment \
  --profile spendsync
```
3. Monitor ECS deployment transition:
```bash
aws ecs wait services-stable \
  --cluster spendsync-dev-cluster \
  --services spendsync-dev-backend \
  --profile spendsync
```

### 4.2. Frontend Rollback
1. Checkout the previous stable Git commit locally:
```bash
git checkout <PREVIOUS_COMMIT_SHA>
```
2. Rebuild and synchronize static assets:
```bash
cd frontend && npm run build
aws s3 sync dist/ s3://spendsync-dev-frontend-<AWS_ACCOUNT_ID> --delete --profile spendsync
```
3. Invalidate CloudFront cache:
```bash
aws cloudfront create-invalidation \
  --distribution-id <DISTRIBUTION_ID> \
  --paths "/*" \
  --profile spendsync
```

---

## 5. Observability and Debugging

### 5.1. Streaming Container Application Logs
Application logs are streamed directly to AWS CloudWatch:

```bash
aws logs tail /ecs/spendsync-dev-backend \
  --follow \
  --format short \
  --profile spendsync
```

Filter for application exceptions:
```bash
aws logs filter-log-events \
  --log-group-name /ecs/spendsync-dev-backend \
  --filter-pattern "ERROR" \
  --profile spendsync
```

### 5.2. Inspecting ECS Service Events
To diagnose task placement, container registration, or health check failures:
```bash
aws ecs describe-services \
  --cluster spendsync-dev-cluster \
  --services spendsync-dev-backend \
  --query "services[0].events[:10]" \
  --output table \
  --profile spendsync
```

### 5.3. Inspecting Application Load Balancer Target Health
```bash
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:<REGION>:<AWS_ACCOUNT_ID>:targetgroup/spendsync-dev-tg/<TARGET_GROUP_ID> \
  --profile spendsync
```

---

## 6. Database Operations

### 6.1. Flyway Schema Migrations
Database migrations are applied automatically during Spring Boot application startup. To inspect applied migration history, query the `flyway_schema_history` table:
```sql
SELECT installed_rank, version, description, type, script, checksum, installed_on, execution_time, success 
FROM flyway_schema_history 
ORDER BY installed_rank DESC;
```

### 6.2. Resolving Checksum Mismatches
If a previously applied migration script is modified locally, Flyway halts startup with a checksum validation error. Never alter an applied migration. Author a new sequential migration script (e.g., `V1_17__fix_description.sql`) to introduce schema updates.

---

## 7. Cost and FinOps Management

### 7.1. Budget Thresholds
The account operates under an AWS Budget threshold of **$20.00 USD / month**:
- 85% threshold ($17.00): Informational email alert.
- 100% threshold ($20.00): Critical notification.
- Forecasted threshold: Triggers when projected end-of-month spend exceeds budget.

### 7.2. Active Cost-Optimization Controls
- ECS Fargate tasks utilize **Fargate Spot** capacity providers, reducing compute rates by ~70%.
- ElastiCache Valkey operates on **Serverless** mode with data limits constrained between 0 GB and 1 GB.
- NAT Gateways are omitted; container pulling and package resolution leverage public subnets and direct routing, saving approximately $32.00 USD / month per gateway.
- S3 asset and state buckets enforce non-current version expiration after 90 days.
