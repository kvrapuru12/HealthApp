# Repository Infrastructure Inventory

**Generated:** Phase A discovery  
**Purpose:** Baseline of in-repo AWS deployment assets before account migration.

## Accounts

| Role | Profile | Account ID | Region |
|------|---------|------------|--------|
| Source | `default` | `114749311002` | `us-east-1` |
| Target | `healthapp-target` | `192033640931` | `us-east-1` |

---

## Terraform (`terraform/`)

Flat single-root module (no submodules). Terraform `>= 1.0`, AWS provider `~> 4.67.0`.

| File | Purpose |
|------|---------|
| `main.tf` | VPC, SGs, RDS, Secrets Manager, SSM, ECR, ECS, ALB, IAM, log group, task definition, service |
| `variables.tf` | Inputs with validation (`db_password` ≥8, `jwt_secret` ≥32) |
| `outputs.tf` | ALB DNS/zone, ECR URL, RDS endpoint, VPC/subnets, ECS names, application URLs |
| `cloudwatch.tf` | 6 metric alarms (no SNS actions) |
| `terraform.tfvars.example` | Template for secrets and Apple client IDs |
| `.gitignore` | Ignores `terraform.tfvars`, state, `.terraform/` |

### Resource count (Terraform-managed)

~45 resources in `main.tf` + 6 alarms in `cloudwatch.tf`:

- **Networking:** VPC `10.0.0.0/16`, 2 public + 2 private subnets, IGW, 1 NAT GW, route tables
- **RDS:** `healthapp-db`, MySQL `8.0.44`, `db.t3.micro`, 20 GB gp2, encrypted
- **ECS:** Fargate cluster/service/task (`512` CPU / `1024` MB), desired count env-based (2 prod / 1 non-prod)
- **ALB:** `healthapp-alb`, HTTP:80 only, health check `/api/actuator/health`
- **ECR:** `healthapp` with lifecycle policy (keep 10 images)
- **Secrets:** `healthapp/db-password`, `healthapp/jwt-secret` (Secrets Manager)
- **SSM:** `/healthapp/openai-api-key`, `/healthapp/usda-api-key` (SecureString)
- **IAM:** `ecsTaskExecutionRole`, `ecsTaskRole`
- **Logs:** `/ecs/healthapp`, 30-day retention

### State backend

S3 backend is **commented out** in `main.tf`. Default is local state. Phase B bootstraps remote state in target account.

---

## Deploy scripts

### `deploy-aws.sh`

- Prerequisites: AWS CLI, Docker, Maven
- `mvn clean package -DskipTests` → `docker build` → ECR login/push (`:latest` + git SHA)
- `aws ecs update-service --force-new-deployment` (does **not** register new task definition)
- Waits for `services-stable`
- Env: `AWS_REGION` (default `us-east-1`); credentials via `AWS_PROFILE` or default chain

### `.github/workflows/deploy-aws-target.yml`

- Trigger: push to any branch, merge to `main`, or `workflow_dispatch`
- Account: target `192033640931` (`AWS_TARGET_*` secrets; verifies caller identity)
- Job 1: `mvn clean verify` + package
- Job 2: build, ECR push (`$GITHUB_SHA` + `latest`), download/register/deploy task definition with SHA image
- Post-deploy: poll `http://<ALB_DNS>/api/actuator/health` (30 retries)

---

## Application AWS config

### `src/main/resources/application-aws.properties`

- JDBC from env: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_PASSWORD`
- **Stale default** `DB_HOST`: `healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com` (overridden by ECS task env at runtime)
- `spring.jpa.hibernate.ddl-auto=none` — schema via Flyway only
- Flyway: `repair-on-migrate=true`, `out-of-order=true`, `ignore-migration-patterns=*:missing`
- Secrets from env: `JWT_SECRET`, `OPENAI_API_KEY`, `USDA_API_KEY`
- Actuator health exposed for ALB checks; server port `8080`; context path `/api` from base `application.properties`

### Flyway migrations

33 versioned SQL files in `src/main/resources/db/migration/` (`V1__init.sql` through `V34__create_food_nutrition_cache.sql`). Migrations run on ECS task startup.

### `Dockerfile`

- Temurin 17, JAR `healthapp-backend-1.0.0.jar`, port 8080
- Health check: `curl -f http://localhost:8080/api/actuator/health`

---

## Production URL (external to repo)

- Production API: `http://api.thanafit.com/api`
- DNS managed in Route 53 (source account) — see `07-dns-cutover.md`
- Not defined in Terraform or application config

---

## Related docs

- `DEPLOYMENT.md` — primary deployment guide (some drift vs live: MySQL version, backup retention)
