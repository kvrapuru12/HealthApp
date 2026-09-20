# Rollback Procedures

**Scope:** Revert traffic to source account without destroying source infrastructure.  
**Principle:** Source account `114749311002` is never modified or deleted during rollback.

---

## Rollback scenarios

| Scenario | Trigger | Action |
|----------|---------|--------|
| **R1: DNS cutover rollback** | Target unhealthy after DNS change | Revert Route 53 alias to source ALB |
| **R2: Pre-cutover abort** | Target validation fails before DNS change | No DNS action; source remains production |
| **R3: Target infra teardown** | Abandon migration entirely | Destroy target Terraform (optional); source unaffected |

---

## R1 — DNS cutover rollback (most common)

Use when `api.thanafit.com` was pointed at target ALB and production is broken.

### Source ALB reference (keep handy)

| Field | Value |
|-------|-------|
| ALB DNS | `healthapp-alb-1571435665.us-east-1.elb.amazonaws.com` |
| ALB hosted zone ID | `Z35SXDOTRQ7X7K` |
| Route 53 zone | `thanafit.com` (`Z09775432VGRB7DELDK5Q`) |

### Steps

1. **Confirm source still healthy** (should be — source was not torn down):
   ```bash
   curl -sf http://healthapp-alb-1571435665.us-east-1.elb.amazonaws.com/api/actuator/health
   ```

2. **Revert Route 53 record** in hosted zone `Z09775432VGRB7DELDK5Q`:
   - Record: `api.thanafit.com`
   - Type: A (alias)
   - Alias target: `dualstack.healthapp-alb-1571435665.us-east-1.elb.amazonaws.com`
   - Target hosted zone ID: `Z35SXDOTRQ7X7K`
   - Evaluate target health: `true`

3. **Verify propagation:**
   ```bash
   dig +short api.thanafit.com
   curl -sf http://api.thanafit.com/api/actuator/health
   ```
   IPs should return to `3.229.100.70`, `54.166.175.90`.

4. **Do not use GitHub Actions to redeploy source.** CI has no source-account workflow or secrets. Rollback is DNS only.

5. **Communicate** — production restored on source; investigate target separately.

### Expected downtime

Alias record updates typically propagate within minutes. No TTL on alias records.

---

## R2 — Pre-cutover abort

Target ALB validation failed; DNS was **never** changed.

### Steps

1. **No DNS changes required** — `api.thanafit.com` still points to source
2. Verify production:
   ```bash
   curl -sf http://api.thanafit.com/api/actuator/health
   ```
3. Debug target using `06-validation.md` checklist
4. Target infra can remain running for debugging (costs apply) or be destroyed later

**Zero production impact** if DNS was not cut over.

---

## R3 — Destroy target infrastructure (optional)

Only if abandoning the target account migration. **Does not affect source.**

```bash
export AWS_PROFILE=healthapp-target
cd terraform
terraform destroy -var-file=terraform.tfvars
```

**Warnings:**
- Deletes RDS (empty schema only if no data migrated)
- Deletes ECR images
- Secrets Manager / SSM parameters removed
- S3 state bucket (if created) should be emptied/versioned before delete

Source account `114749311002` resources remain untouched.

---

## What we never do on rollback

| Action | Why forbidden |
|--------|---------------|
| Delete source RDS | Production data lives there |
| Delete source ECS/ALB | Production traffic may still use it |
| Modify source secrets | Breaks live app |
| Change DNS without approval | Governance / surprise outage |

---

## Rollback decision matrix

```
                    DNS cut over?
                         │
            ┌────────────┴────────────┐
           No                       Yes
            │                         │
     R2: fix target            R1: revert DNS
     source = prod             to source ALB
            │                         │
            └────────────┬────────────┘
                         │
              Target infra keep or destroy (R3)
```

---

## Post-rollback follow-up

1. Document incident in migration log
2. Root-cause target failure (ECS logs, RDS, Flyway, secrets)
3. Re-validate target ALB before retrying cutover
4. Consider keeping target infra for parallel testing without DNS change
