# ADR 002: CloudFront Unified Origin and Reverse Proxy for API Routing

## Status
Accepted

## Context
The SpendSync frontend is a React single-page application compiled into static assets and hosted in a private Amazon S3 bucket behind Amazon CloudFront. The backend API is served by an Amazon ECS Fargate service exposed through an Application Load Balancer (ALB).

In early deployment, the frontend accessed the backend API directly via the public DNS hostname of the Application Load Balancer over plain HTTP (`http://spendsync-dev-alb-<ALB_ID>.<REGION>.elb.amazonaws.com`). 

This architecture produced two critical operational problems:
1. **Mixed Content Blocking:** Modern web browsers block unencrypted HTTP network requests (`XMLHttpRequest` / `fetch`) when initiated from an HTTPS origin (`https://d111111abcdef8.cloudfront.net`).
2. **Cross-Origin Resource Sharing (CORS):** Cross-origin requests between the CloudFront distribution domain and the ALB domain required explicit preflight (`OPTIONS`) handling, browser certificate trust, and CORS header management on the Spring Boot backend.

## Decision Drivers
1. Eliminate Mixed Content security violations across all modern browsers.
2. Terminate all client-facing traffic on TLS (HTTPS) without purchasing or configuring custom domain certificates for the development ALB.
3. Eliminate cross-origin preflight latency and CORS configuration maintenance on the backend.
4. Maintain a unified entry point for both static assets and API requests.

## Considered Options
- **Option 1: Procure and Attach an ACM Certificate to the ALB:** Requires owning a custom domain registered in Route 53 or validated through DNS, adding domain ownership dependencies and certificate renewal configurations.
- **Option 2: Proxy API Requests through an Nginx Container:** Requires running and scaling an additional proxy container in ECS, increasing compute costs and points of failure.
- **Option 3: Configure CloudFront with Path-Based Routing to the ALB:** Define an additional cache behavior in CloudFront matching the `/api/*` path pattern, forwarding requests directly to the ALB origin.

## Decision Outcome
Chosen option: **Option 3 (CloudFront Path-Based Reverse Proxy)**.

The CloudFront distribution is configured with two origins:
1. **Primary Origin (S3 Bucket):** Serves default traffic (`/*`) for static assets with Origin Access Control (OAC).
2. **Secondary Origin (ALB):** Routes `/api/*` directly to the Application Load Balancer over HTTP port 80.

### Cache Behavior Configuration
- **Path Pattern:** `/api/*`
- **Target Origin:** ALB DNS name
- **Viewer Protocol Policy:** `redirect-to-https`
- **Allowed HTTP Methods:** `GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE`
- **Cache Policy:** `Managed-CachingDisabled` (all API requests pass directly to the backend without edge caching)
- **Origin Request Policy:** `Managed-AllViewerExceptHostHeader` (preserves client headers, authentication tokens, and tenant IDs while ensuring the ALB receives its own Host header)

### Consequences

#### Positive
- Both frontend assets and backend API requests share the same origin (`https://d111111abcdef8.cloudfront.net`), eliminating CORS preflight overhead and browser security restrictions.
- Browser-to-CloudFront traffic is fully encrypted using the default CloudFront wildcard SSL certificate (`*.cloudfront.net`), removing the need for a custom domain certificate during development.
- Single global endpoint simplifies client configuration (`VITE_API_BASE_URL=""`).

#### Negative
- All API traffic passes through CloudFront before reaching the ALB, introducing a minor routing hop (typically 5–15 ms).
- WebSocket or streaming connections would require specific CloudFront protocol configurations if introduced in future requirements.
