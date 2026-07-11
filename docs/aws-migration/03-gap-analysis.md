# Gap Analysis — Source vs Target vs Repo

**Source:** `114749311002` (`default`)  
**Target:** `192033640931` (`healthapp-target`)  
**Region:** `us-east-1`

---

## Target pre-flight (read-only)

| Resource | Target status |
|----------|---------------|
| VPC `healthapp-vpc` | **Not found** — clean |
| RDS `healthapp-db` | **Not found** — clean |
| ALB `healthapp-alb` | **Not found** — clean |
| ECR `healthapp` | **Not found** — clean |
| ECS `healthapp-cluster` | **Not found** — clean |
| Secrets `healthapp/*` | **Not found** — clean |

Target account is empty for HealthApp resources. No naming conflicts expected.

**IAM note:** Target credentials use **root** (`arn:aws:iam::192033640931:root`). Works for bootstrap; replace with IAM deploy user post-migration.

---

## Gaps: Infrastructure as Code vs Live Source

| Area | Repo / Terraform | Live source | Action for target |
|------|------------------|-------------|-------------------|
| RDS backup retention | 7 days | **1 day** | Match Terraform (7) or keep MVP at 1 via tfvars |
| ECS desired count | 2 (production default) | **1 task** | Use `ecs_desired_count = 1` in tfvars (matches live) |
| RDS engine | 8.0.44 | 8.0.44 | Upgrade to **MySQL 8.4.x** on target (empty DB, Flyway-only) |
| OpenAI secret store | SSM only | SSM + extra Secrets Manager copies | Terraform creates SSM only |
| ALB HTTPS | HTTP:80 only | HTTP:80 only | Same; DNS cutover stays HTTP unless ACM added later |
| Terraform state | Local (commented S3 backend) | N/A | Bootstrap S3 + DynamoDB in **target** account |
| CloudWatch alarms | 6 alarms, no SNS | Same | Recreate; optionally add SNS post-migration |
| `application-aws.properties` default DB host | Stale legacy endpoint | Overridden by ECS env | No change required for migration |

---

## Gaps: Not in Terraform (manual / external)

| Item | Current state | Migration impact |
|------|---------------|------------------|
| Route 53 `thanafit.com` | Source account, zone `Z09775432VGRB7DELDK5Q` | DNS cutover doc only; **no change until approved** |
| `api.thanafit.com` | Alias → source ALB | Update alias to target ALB after validation |
| ACM certificate | Validation CNAME exists for `api.thanafit.com` | May need re-validation if HTTPS added later |
| GitHub Actions secrets | Point to source IAM user | Update to target deploy credentials after Phase B |
| DB data | Full production data on source RDS | **Out of scope** — empty RDS on target; separate migration |

---

## Gaps: Deploy path inconsistencies

| Path | Behavior |
|------|----------|
| `deploy-aws.sh` | Pushes `:latest`, force-new-deployment only |
| GitHub Actions | Registers task def with **SHA** image tag |

Both work; CI gradually moves task definition off `:latest`. Target deploy can use either; recommend `deploy-aws.sh` for initial bootstrap, then update GitHub secrets.

---

## RDS version decision

| Option | Recommendation |
|--------|----------------|
| MySQL 8.4.x | **Preferred** for target — empty DB, Flyway creates schema; 8.0 standard support ends 2026-07-31 |
| MySQL 8.0.44 | Only if planning immediate dump/restore from source |

Phase B will set `engine_version` to latest available 8.4.x in `us-east-1`.

---

## Security gaps

1. Target uses root access keys — create `healthapp-deploy` IAM user after migration
2. Secrets in Terraform state when applied — protect S3 state bucket with encryption + versioning
3. Source root/default keys should be rotated if ever exposed during setup

---

## Risk summary

| Risk | Mitigation |
|------|------------|
| Accidental source changes | All source ops read-only; Phase B uses `healthapp-target` profile only |
| DNS cutover downtime | Lower TTL before cutover; validate target ALB first |
| Empty DB / no users on target | Expected; Flyway schema only; data migration separate |
| JWT secret reuse | Existing mobile tokens valid until expiry if same secret used — acceptable for parallel testing |
