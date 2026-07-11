# HealthApp AWS Account Migration

Migrate infrastructure from **source** `114749311002` to **target** `192033640931` in `us-east-1`.  
Production URL: `http://api.thanafit.com/api` (DNS unchanged until approved).

## Phase status

| Phase | Status | Description |
|-------|--------|-------------|
| **A** | **Complete** | Read-only discovery, gap analysis, terraform plan (no AWS writes) |
| **B** | Pending approval | Provision target infra, deploy, validate target ALB |

## Documentation index

| Doc | Contents |
|-----|----------|
| [01-inventory-repo.md](01-inventory-repo.md) | Terraform, deploy scripts, Flyway, app AWS config |
| [02-inventory-source-aws.md](02-inventory-source-aws.md) | Live source account resources (read-only) |
| [03-gap-analysis.md](03-gap-analysis.md) | Source vs target vs repo gaps |
| [04-migration-plan.md](04-migration-plan.md) | Terraform plan summary, Phase B execution order |
| [05-secrets-cicd.md](05-secrets-cicd.md) | Secrets paths, tfvars, GitHub Actions rotation |
| [06-validation.md](06-validation.md) | Pre/post apply validation checklists |
| [07-dns-cutover.md](07-dns-cutover.md) | Route 53 wiring + cutover steps (approval-gated) |
| [08-rollback.md](08-rollback.md) | DNS revert and abort procedures |

## AWS profiles

| Profile | Account | Use |
|---------|---------|-----|
| `default` | `114749311002` | Source inventory (read-only) |
| `healthapp-target` | `192033640931` | Target plan / apply / deploy |

## Phase A summary

- **Terraform plan:** `45 to add, 0 to change, 0 to destroy` (target, fresh state)
- **Target account:** Empty — no naming conflicts
- **DNS:** `api.thanafit.com` → source ALB `healthapp-alb-1571435665.us-east-1.elb.amazonaws.com`
- **Source state backup:** `terraform/terraform.tfstate.source-114749311002.backup`
- **Secrets:** `terraform/terraform.tfvars` prepared (gitignored, not in docs)
- **No changes made** to source resources or DNS

## Phase B status

| Step | Status |
|------|--------|
| Terraform backend + apply (45 resources) | Done |
| Target ALB | `healthapp-alb-1602639566.us-east-1.elb.amazonaws.com` |
| Target RDS | `healthapp-db.ci124w0qqo5j.us-east-1.rds.amazonaws.com:3306` |
| App deploy (ECR image + ECS healthy) | **Pending** — run target GitHub Action |

### Deploy target app (no local Docker)

1. Add GitHub secrets: `AWS_TARGET_ACCESS_KEY_ID`, `AWS_TARGET_SECRET_ACCESS_KEY` (target account IAM user)
2. Merge/push `.github/workflows/deploy-aws-target.yml`
3. Actions → **Deploy HealthApp to AWS (Target Account)** → Run workflow
4. Validate `http://healthapp-alb-1602639566.us-east-1.elb.amazonaws.com/api/actuator/health`

See [05-secrets-cicd.md](05-secrets-cicd.md).

## Hard constraints

- No DB data migration in this project
- No source resource modifications
- No DNS change without explicit approval
- No secrets in committed files or documentation
