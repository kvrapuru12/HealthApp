# HealthApp AWS Account Migration

Migrate infrastructure from **source** `114749311002` to **target** `192033640931` in `us-east-1`.  
Production URL: `http://api.thanafit.com/api` (DNS unchanged until approved).

## Phase status

| Phase | Status | Description |
|-------|--------|-------------|
| **A** | **Complete** | Read-only discovery, gap analysis, terraform plan |
| **B** | **Complete** | Target infra, GitHub deploy, target ALB health 200 |
| **DNS cutover** | Pending approval | `api.thanafit.com` still points to source |

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

## Phase B status (complete)

| Step | Status |
|------|--------|
| Terraform backend + apply (45 resources) | Done |
| IAM user `healthapp-github-deploy` + GitHub secrets | Done |
| Target GitHub Action deploy | Done ([run 29151330815](https://github.com/kvrapuru12/HealthApp/actions/runs/29151330815)) |
| Target ALB health | **HTTP 200** — `{"status":"UP"}` |
| Target RDS | `healthapp-db.ci124w0qqo5j.us-east-1.rds.amazonaws.com:3306` |
| Production `api.thanafit.com` | **Unchanged** — HTTP 200 via source ALB |

### Target URLs (until DNS cutover)

- API: http://healthapp-alb-1602639566.us-east-1.elb.amazonaws.com/api
- Health: http://healthapp-alb-1602639566.us-east-1.elb.amazonaws.com/api/actuator/health

### Next: DNS cutover (approval required)

See [07-dns-cutover.md](07-dns-cutover.md). Do not change `api.thanafit.com` until you approve.

## Hard constraints

- No DB data migration in this project
- No source resource modifications
- No DNS change without explicit approval
- No secrets in committed files or documentation
