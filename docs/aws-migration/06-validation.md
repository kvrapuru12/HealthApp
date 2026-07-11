# Validation Checklist

**Phase A:** Pre-apply checklist (run against source for baseline)  
**Phase B:** Post-apply validation on **target ALB only** (not `api.thanafit.com` until DNS cutover approved)

---

## Phase A — completed validations (read-only)

| Check | Command / method | Result |
|-------|------------------|--------|
| Source profile identity | `aws sts get-caller-identity --profile default` | Account `114749311002` |
| Target profile identity | `aws sts get-caller-identity --profile healthapp-target` | Account `192033640931` |
| Source ALB healthy | Target group state | **healthy** (`10.0.3.99:8080`) |
| Production DNS | `api.thanafit.com` resolves to source ALB IPs | `3.229.100.70`, `54.166.175.90` |
| Target empty | No `healthapp-*` resources in target | **Clean** |
| Terraform plan | `terraform plan` with `healthapp-target` + fresh state | **45 to add**, 0 change, 0 destroy |

---

## Phase B — infrastructure validation (after `terraform apply`)

Run with `export AWS_PROFILE=healthapp-target AWS_REGION=us-east-1`

### Terraform outputs

```bash
cd terraform
terraform output alb_dns_name
terraform output alb_zone_id
terraform output rds_endpoint
terraform output ecr_repository_url
```

Record `rds_endpoint` in `04-migration-plan.md` for separate DB migration handoff.

### AWS resource checks

| Resource | Command | Expected |
|----------|---------|----------|
| VPC | `aws ec2 describe-vpcs --filters Name=tag:Name,Values=healthapp-vpc` | 1 VPC, `10.0.0.0/16` |
| RDS | `aws rds describe-db-instances --db-instance-identifier healthapp-db` | `available`, MySQL 8.4.x |
| ECS cluster | `aws ecs describe-clusters --clusters healthapp-cluster` | ACTIVE |
| ECS service | `aws ecs describe-services --cluster healthapp-cluster --services healthapp-service` | runningCount = desiredCount |
| ALB | `aws elbv2 describe-load-balancers --names healthapp-alb` | active |
| ECR | `aws ecr describe-repositories --repository-names healthapp` | exists |
| Secrets | `aws secretsmanager list-secrets --filters Key=name,Values=healthapp` | db-password, jwt-secret |
| SSM | `aws ssm get-parameters-by-path --path /healthapp` | openai-api-key, usda-api-key |
| Logs | `aws logs describe-log-groups --log-group-name-prefix /ecs/healthapp` | 30-day retention |

---

## Phase B — application validation (2026-07-11)

Target deploy via GitHub Actions workflow **Deploy HealthApp to AWS (Target Account)**.

| Check | Result |
|-------|--------|
| Target ALB health | **HTTP 200** — `status: UP`, db: UP |
| ECS running/desired | 1 / 1 |
| ECR image tags | `latest`, commit SHA |
| Production api.thanafit.com | **HTTP 200** (source, unchanged) |

Target ALB DNS: `healthapp-alb-1602639566.us-east-1.elb.amazonaws.com`

---

## Phase B — application validation (reference commands)

Replace `<ALB_DNS>` with `terraform output -raw alb_dns_name`.

### Health endpoint

```bash
ALB_DNS=$(cd terraform && terraform output -raw alb_dns_name)

# Basic health
curl -sf "http://${ALB_DNS}/api/actuator/health" | jq .

# HTTP status
curl -s -o /dev/null -w "%{http_code}" "http://${ALB_DNS}/api/actuator/health"
# Expected: 200
```

### Extended checks

| Endpoint | URL | Expected |
|----------|-----|----------|
| Health | `http://<ALB_DNS>/api/actuator/health` | HTTP 200, `"status":"UP"` |
| Swagger | `http://<ALB_DNS>/api/swagger-ui/index.html` | HTTP 200 |
| API docs | `http://<ALB_DNS>/api/api-docs` | HTTP 200 |

### ECS / logs

```bash
aws ecs describe-services --cluster healthapp-cluster --services healthapp-service \
  --query 'services[0].{status:status,running:runningCount,desired:desiredCount}'

aws logs tail /ecs/healthapp --since 15m --format short
```

**Flyway:** On first successful task start, logs should show Flyway migrations applied (V1–V34). Empty RDS → full schema created.

### Database connectivity

From ECS logs, confirm no `Communications link failure` or auth errors. Health actuator `db` component should be `UP`.

---

## Phase B — negative tests (optional)

| Test | Action | Expected |
|------|--------|----------|
| Source unchanged | `curl -sf http://api.thanafit.com/api/actuator/health` | Still 200 (DNS not cut over) |
| Target isolation | Target ALB not in Route 53 | Only reachable via ALB DNS directly |

---

## DNS cutover validation (post-approval only)

Only after explicit approval — see `07-dns-cutover.md`.

```bash
dig +short api.thanafit.com
curl -sf http://api.thanafit.com/api/actuator/health
```

Compare resolved IPs to **target** ALB, not source.

---

## Sign-off template

| Gate | Owner | Date | Pass? |
|------|-------|------|-------|
| Terraform apply complete | | | |
| ECS service stable | | | |
| Target ALB health 200 | | | |
| Flyway migrations OK | | | |
| `rds_endpoint` recorded | | | |
| DNS cutover (if approved) | | | |
