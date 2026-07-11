# Migration Execution Plan

**Target account:** `192033640931` (`healthapp-target`)  
**Region:** `us-east-1`  
**Phase A status:** **Complete** (all docs in `docs/aws-migration/`)  
**Phase B status:** Pending explicit approval — no `terraform apply` yet

---

## Terraform plan summary (target, fresh state)

```
Plan: 45 to add, 0 to change, 0 to destroy
```

Profile: `healthapp-target`  
State: Fresh local state (source state backed up to `terraform.tfstate.source-114749311002.backup`)

### Resources to create (high level)

| Category | Resources |
|----------|-----------|
| Networking | VPC, 4 subnets, IGW, NAT GW, EIP, 2 route tables, 4 associations |
| Security | 3 security groups (ALB, ECS, RDS) |
| RDS | DB subnet group, `healthapp-db` MySQL instance |
| Secrets | 2 Secrets Manager secrets + versions |
| SSM | 2 SecureString parameters |
| ECR | Repository + lifecycle policy |
| ECS | Cluster, task definition, service |
| IAM | ecsTaskExecutionRole, ecsTaskRole + policies |
| ALB | Load balancer, target group, HTTP listener |
| CloudWatch | Log group + 6 metric alarms |

### Outputs after apply

| Output | Purpose |
|--------|---------|
| `alb_dns_name` | Target API URL until DNS cutover |
| `alb_zone_id` | Route 53 alias cutover |
| `rds_endpoint` | **Handoff for separate DB data migration** |
| `ecr_repository_url` | Container push target |
| `application_urls` | Health check, Swagger URLs |

---

## State management

| Item | Detail |
|------|--------|
| Source local state | Backed up: `terraform/terraform.tfstate.source-114749311002.backup` |
| Target plan state | Fresh `terraform.tfstate` (empty → plan only) |
| Phase B backend | S3 `healthapp-terraform-state-192033640931` + DynamoDB `terraform-state-lock` |

**Important:** Do not run `terraform apply` with source profile against backed-up state in target account.

---

## Phase B execution order

1. Bootstrap Terraform remote state (S3 + DynamoDB) in target
2. Update `terraform/main.tf`: S3 backend + MySQL 8.4.x engine
3. `terraform init -reconfigure` → `terraform apply`
4. `AWS_PROFILE=healthapp-target ./deploy-aws.sh`
5. Validate `http://<target-alb>/api/actuator/health`
6. Record `rds_endpoint` below after apply

---

## RDS configuration (Phase B)

| Setting | Phase A plan | Phase B apply |
|---------|--------------|---------------|
| Engine | mysql | mysql |
| Version | 8.0.44 (repo default) | **8.4.x** (preferred) |
| Instance | db.t3.micro | db.t3.micro |
| Storage | 20 GB gp2 | 20 GB gp2 |
| Database name | healthapp | healthapp |
| Public access | false (production) | false |
| Schema | — | Flyway on first ECS deploy |

### Target RDS endpoint (Phase B apply complete)

```
RDS_ENDPOINT=healthapp-db.ci124w0qqo5j.us-east-1.rds.amazonaws.com:3306
```

Use this host for a **separate DB data migration** (dump/restore or DMS). Schema already created by Flyway on first deploy.

---

## Variables used (`terraform.tfvars`)

Populated from source account secrets (values not stored in this doc):

- `db_password`, `jwt_secret` — Secrets Manager
- `openai_api_key`, `usda_api_key` — SSM
- `environment = "production"`
- `ecs_desired_count = 1` (matches live source)
- Apple client IDs: `com.prod.thanafit`

File: `terraform/terraform.tfvars` (gitignored)

---

## Phase A deliverables (complete)

| # | Document | Status |
|---|----------|--------|
| 01 | `01-inventory-repo.md` | Done |
| 02 | `02-inventory-source-aws.md` | Done |
| 03 | `03-gap-analysis.md` | Done |
| 04 | `04-migration-plan.md` | Done |
| 05 | `05-secrets-cicd.md` | Done |
| 06 | `06-validation.md` | Done |
| 07 | `07-dns-cutover.md` | Done |
| 08 | `08-rollback.md` | Done |
| — | `README.md` | Done |

## Approval gate

**Phase A is complete.** Phase B provisions real resources in target account `192033640931`. Source account untouched. DNS unchanged. Say **"Proceed with Phase B"** to continue.
