# Secrets & CI/CD Configuration

**Phase:** A (documentation) — no secrets values stored here  
**Source account:** `114749311002` | **Target account:** `192033640931`

---

## Runtime secrets (ECS task)

Terraform provisions secrets in the **target** account on `terraform apply`. ECS injects them as environment variables at task start.

| Runtime env var | AWS store | Path / name | Required |
|-----------------|-----------|-------------|----------|
| `DB_PASSWORD` | Secrets Manager | `healthapp/db-password` | Yes |
| `JWT_SECRET` | Secrets Manager | `healthapp/jwt-secret` | Yes (≥32 chars) |
| `OPENAI_API_KEY` | SSM Parameter Store | `/healthapp/openai-api-key` (SecureString) | Yes (voice AI) |
| `USDA_API_KEY` | SSM Parameter Store | `/healthapp/usda-api-key` (SecureString) | Optional |

Java reads these via `${ENV_VAR}` placeholders in `application-aws.properties` — no AWS SDK at runtime.

### Plain ECS environment (not secrets)

| Variable | Source |
|----------|--------|
| `SPRING_PROFILES_ACTIVE` | `aws` |
| `DB_HOST` | RDS endpoint from Terraform |
| `DB_USERNAME` | `admin` |
| `DB_NAME` | `healthapp` |
| `DB_PORT` | `3306` |
| `APPLE_CLIENT_ID` / `APPLE_CLIENT_ID_IOS` | Terraform vars |

---

## Terraform apply-time secrets (`terraform.tfvars`)

File: `terraform/terraform.tfvars` (**gitignored** — do not commit)

Created in Phase A by copying values from **source** account (read-only):

```hcl
aws_region         = "us-east-1"
environment        = "production"
db_password        = "<from source Secrets Manager healthapp/db-password>"
jwt_secret         = "<from source Secrets Manager healthapp/jwt-secret>"
openai_api_key     = "<from source SSM /healthapp/openai-api-key>"
usda_api_key       = "<from source SSM /healthapp/usda-api-key>"
apple_client_id_ios = "com.prod.thanafit"
apple_client_id     = "com.prod.thanafit"
ecs_desired_count   = 1
```

### Source account secret inventory (metadata only)

| Store | Name | Source ARN suffix |
|-------|------|-------------------|
| Secrets Manager | `healthapp/db-password` | `...-W7FEsE` |
| Secrets Manager | `healthapp/jwt-secret` | `...-j5x8zE` |
| SSM | `/healthapp/openai-api-key` | SecureString |
| SSM | `/healthapp/usda-api-key` | SecureString |

**Source drift:** Source also has `healthapp/openai-api-key` and `healthapp/openai-api-key-plain` in Secrets Manager (not used by Terraform/ECS). Target will use SSM only per Terraform.

### Reuse policy

| Secret | Reuse from source? | Notes |
|--------|-------------------|-------|
| `db_password` | Yes | Same password simplifies future DB dump/restore |
| `jwt_secret` | Yes | Empty target DB; no live sessions yet |
| `openai_api_key` | Yes | Third-party key, account-agnostic |
| `usda_api_key` | Yes | Third-party key, account-agnostic |

---

## Terraform state and secrets

`terraform apply` writes secret values into **Terraform state**. Phase B must use:

- S3 backend with encryption + versioning
- Bucket: `healthapp-terraform-state-192033640931` (planned)
- DynamoDB lock: `terraform-state-lock`
- Restrict bucket access to deploy IAM principal only

---

## GitHub Actions CI/CD

Two workflows — **source production** and **target migration** stay separate.

| Workflow | File | Trigger | AWS account |
|----------|------|---------|-------------|
| Production | `.github/workflows/deploy.yml` | Push to `main` | Source `114749311002` |
| Target (migration) | `.github/workflows/deploy-aws-target.yml` | **Manual only** (`workflow_dispatch`) | Target `192033640931` |

### GitHub secrets — production (unchanged)

| GitHub secret | Purpose |
|---------------|---------|
| `AWS_ACCESS_KEY_ID` | Deploy IAM user in **source** `114749311002` |
| `AWS_SECRET_ACCESS_KEY` | Matching secret |

### GitHub secrets — target (add these)

| GitHub secret | Purpose |
|---------------|---------|
| `AWS_TARGET_ACCESS_KEY_ID` | Deploy IAM user in **target** `192033640931` |
| `AWS_TARGET_SECRET_ACCESS_KEY` | Matching secret |

Create IAM user `healthapp-deploy` in target account (avoid long-term root keys). Attach policy: ECR push/pull, ECS describe/register/update, `elbv2:DescribeLoadBalancers`, CloudWatch Logs read.

The target workflow verifies `sts get-caller-identity` equals `192033640931` before deploy.

### Run target deploy (no local Docker)

1. GitHub → **Actions** → **Deploy HealthApp to AWS (Target Account)**
2. **Run workflow** (branch: `main`)
3. Wait for green — health check hits target ALB only (not `api.thanafit.com`)

Optional input: `skip_tests` for emergency redeploys only.

### Deploy paths

| Method | Profile / creds | Image tag | Task definition |
|--------|-----------------|-----------|-----------------|
| `deploy-aws-target.yml` | `AWS_TARGET_*` secrets | `$GITHUB_SHA` + `:latest` | Registers new revision |
| `deploy.yml` | `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | `$GITHUB_SHA` + `:latest` | Source production |
| `deploy-aws.sh` | `AWS_PROFILE=healthapp-target` | `:latest` + git SHA | Force redeploy only (local Docker) |

**Recommendation:** Use `deploy-aws-target.yml` for target bootstrap and ongoing target deploys until DNS cutover.

---

## Local development (not used in AWS)

- `.env` at project root (gitignored): `OPENAI_API_KEY`, `USDA_API_KEY`, `DB_PASSWORD`, `JWT_SECRET`
- `DotenvEnvironmentPostProcessor` loads `.env` for local `spring-boot:run` only

---

## Security checklist (Phase B+)

- [ ] Replace target root keys with IAM deploy user
- [ ] Enable MFA on target root account
- [ ] Confirm `terraform.tfvars` is not committed
- [ ] S3 state bucket blocks public access
- [ ] Rotate keys if exposed during setup
