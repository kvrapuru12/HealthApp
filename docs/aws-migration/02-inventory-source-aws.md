# Source AWS Inventory (Read-Only)

**Account:** `114749311002`  
**Profile:** `default`  
**Region:** `us-east-1`  
**Discovered:** Phase A (no modifications made)

---

## Summary

| Resource | Identifier | Status |
|----------|------------|--------|
| VPC | `vpc-01739f4aaac8dc6ba` (`healthapp-vpc`, `10.0.0.0/16`) | available |
| RDS | `healthapp-db` | available |
| ECS cluster | `healthapp-cluster` | ACTIVE, 1 Fargate task |
| ECS service | `healthapp-service` | ACTIVE, desired=1, running=1 |
| Task definition | `healthapp-task:145` | in use |
| ALB | `healthapp-alb` | active |
| ECR | `healthapp` | exists |
| Route 53 zone | `thanafit.com` (`Z09775432VGRB7DELDK5Q`) | 10 records |

---

## VPC & Networking

| Name | Subnet ID | CIDR | AZ | Public |
|------|-----------|------|-----|--------|
| healthapp-public-1a | subnet-086135edb4c380c8f | 10.0.1.0/24 | us-east-1a | yes |
| healthapp-public-1b | subnet-0350ced83a2575ba1 | 10.0.2.0/24 | us-east-1b | yes |
| healthapp-private-1a | subnet-08697f9f6147ca6dd | 10.0.3.0/24 | us-east-1a | no |
| healthapp-private-1b | subnet-0b2bdf72116405208 | 10.0.4.0/24 | us-east-1b | no |

Matches Terraform CIDR layout in `terraform/main.tf`.

---

## RDS MySQL

| Attribute | Live value | Terraform default |
|-----------|------------|-------------------|
| Identifier | `healthapp-db` | `healthapp-db` |
| Endpoint | `healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com` | — |
| Engine | mysql | mysql |
| Version | **8.0.44** | **8.0.44** |
| Class | db.t3.micro | db.t3.micro |
| Storage | 20 GB | 20 GB |
| Multi-AZ | false | false |
| Publicly accessible | **false** | false (production) |
| Backup retention | **1 day** | **7 days** (drift) |
| Status | available | — |

---

## ECS Fargate

| Attribute | Value |
|-----------|-------|
| Cluster ARN | `arn:aws:ecs:us-east-1:114749311002:cluster/healthapp-cluster` |
| Service | `healthapp-service` — ACTIVE |
| Desired / running | 1 / 1 |
| Task definition | `arn:aws:ecs:us-east-1:114749311002:task-definition/healthapp-task:145` |
| Target group | `arn:aws:elasticloadbalancing:us-east-1:114749311002:targetgroup/healthapp-tg/2bdf3906f5dceec6` |
| Container port | 8080 |

**Drift:** Terraform default `ecs_desired_count` for production is 2; live runs **1 task** (cost optimization).

---

## Application Load Balancer

| Attribute | Value |
|-----------|-------|
| Name | `healthapp-alb` |
| DNS | `healthapp-alb-1571435665.us-east-1.elb.amazonaws.com` |
| Hosted zone ID | `Z35SXDOTRQ7X7K` |
| Scheme | internet-facing |
| Listener | HTTP:80 only (no HTTPS listener) |
| Target health | 1 healthy target (`10.0.3.99:8080`, us-east-1a) |
| Resolved IPs | `3.229.100.70`, `54.166.175.90` |

---

## ECR

| Attribute | Value |
|-----------|-------|
| Repository | `healthapp` |
| URI | `114749311002.dkr.ecr.us-east-1.amazonaws.com/healthapp` |
| Created | 2025-07-25 |

---

## Secrets & Parameters

### Secrets Manager

| Name | ARN |
|------|-----|
| `healthapp/db-password` | `arn:aws:secretsmanager:us-east-1:114749311002:secret:healthapp/db-password-W7FEsE` |
| `healthapp/jwt-secret` | `arn:aws:secretsmanager:us-east-1:114749311002:secret:healthapp/jwt-secret-j5x8zE` |
| `healthapp/openai-api-key` | `arn:aws:secretsmanager:us-east-1:114749311002:secret:healthapp/openai-api-key-VSgLzB` |
| `healthapp/openai-api-key-plain` | `arn:aws:secretsmanager:us-east-1:114749311002:secret:healthapp/openai-api-key-plain-wnbzQs` |

**Note:** Terraform provisions OpenAI key in **SSM**; source also has duplicate OpenAI entries in Secrets Manager (legacy/manual).

### SSM Parameter Store

| Path | Type |
|------|------|
| `/healthapp/openai-api-key` | SecureString |
| `/healthapp/usda-api-key` | SecureString |

ECS task definition injects: `DB_PASSWORD`, `JWT_SECRET` from Secrets Manager; `OPENAI_API_KEY`, `USDA_API_KEY` from SSM.

---

## IAM Roles

| Role | ARN | Created |
|------|-----|---------|
| ecsTaskExecutionRole | `arn:aws:iam::114749311002:role/ecsTaskExecutionRole` | 2025-07-25 |
| ecsTaskRole | `arn:aws:iam::114749311002:role/ecsTaskRole` | 2025-12-25 |

---

## CloudWatch

### Log group

| Name | Retention |
|------|-----------|
| `/ecs/healthapp` | 30 days |

### Alarms (prefix `healthapp-`)

| Alarm | State |
|-------|-------|
| healthapp-ecs-cpu-high | OK |
| healthapp-ecs-memory-high | OK |
| healthapp-alb-response-time | OK |
| healthapp-alb-5xx-errors | INSUFFICIENT_DATA |
| healthapp-rds-cpu-high | OK |
| healthapp-rds-connections-high | OK |

All alarms have empty `alarm_actions` (no SNS/email).

---

## Route 53 (source account)

Hosted zone: **thanafit.com** (`Z09775432VGRB7DELDK5Q`), public, 10 records.

**api.thanafit.com** — A alias record:

| Field | Value |
|-------|-------|
| Type | A (alias) |
| Target | `dualstack.healthapp-alb-1571435665.us-east-1.elb.amazonaws.com` |
| ALB zone ID | `Z35SXDOTRQ7X7K` |
| Evaluate target health | true |

ACM validation CNAME also present for `api.thanafit.com` (certificate workflow).

Public DNS resolves `api.thanafit.com` → `3.229.100.70`, `54.166.175.90` (matches source ALB).

---

## Production health (source)

```bash
curl -s -o /dev/null -w "%{http_code}" http://api.thanafit.com/api/actuator/health
# Expected: 200 (via Route 53 → source ALB)
```

---

## Items not modified (Phase A guarantee)

No create, update, or delete operations were performed on source account resources.
