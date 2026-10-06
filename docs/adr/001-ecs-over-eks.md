# ADR 001: Selection of AWS ECS Fargate Over AWS EKS

## Status
Accepted

## Context
SpendSync is architected as a modular monolith using Spring Boot 3.3 and Java 21, coupled with a React single-page application. When evaluating container orchestration platforms on AWS, the primary alternatives considered were Amazon Elastic Kubernetes Service (EKS) and Amazon Elastic Container Service (ECS) with AWS Fargate.

The platform requires:
- Predictable infrastructure costs aligned with early-stage operational budgets.
- Minimal operational overhead for cluster administration, node patching, and control plane maintenance.
- Fast, reliable container deployments integrated directly with continuous deployment pipelines.
- Capability to execute isolated background tasks and scale horizontally as load increases.

## Decision Drivers
1. **Financial Overhead:** Amazon EKS charges a fixed control plane fee of $0.10 per hour (~$73/month per cluster), exclusive of compute, load balancer, and networking costs. For a development environment, this incurs unnecessary baseline expenditure.
2. **Operational Complexity:** Operating Kubernetes requires managing control plane version upgrades, node group scaling, Custom Resource Definitions (CRDs), Ingress controllers, and cluster networking (CNI/CoreDNS).
3. **Application Topology:** SpendSync is structured as a modular monolith running in a single deployable container alongside external managed data stores (PostgreSQL and Redis/Valkey). A full Kubernetes control plane introduces redundant orchestration layers for a single-service architecture.

## Considered Options
- **Option 1: Amazon EKS (Managed Kubernetes)**
- **Option 2: Amazon ECS with EC2 Launch Type**
- **Option 3: Amazon ECS with Fargate / Fargate Spot Launch Type**

## Decision Outcome
Chosen option: **Option 3 (Amazon ECS with Fargate Spot)**.

ECS Fargate eliminates EC2 instance provisioning and operating system patching. Using Fargate Spot provides an approximate 70% reduction in compute cost compared to on-demand pricing, fitting development budget constraints while providing native integration with AWS Application Load Balancers, IAM task roles, CloudWatch Logs, and SSM Parameter Store.

### Consequences

#### Positive
- Zero fixed cluster control plane cost.
- Native integration with AWS IAM, CloudWatch, and Application Load Balancers without third-party operators.
- Simplified continuous delivery: container image deployments require updating task definitions and calling the ECS service update API.
- Straightforward configuration through Terraform without needing Helm or Kubernetes manifest management.

#### Negative
- Advanced Kubernetes-native tooling such as ArgoCD, Flagger, and service meshes (Istio/Linkerd) cannot be used directly.
- Multi-cloud portability is reduced, as ECS task definitions are specific to AWS.
- Canary deployments require AWS CodeDeploy rather than Kubernetes ingress traffic shifting.
