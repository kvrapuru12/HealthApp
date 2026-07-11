# DNS Cutover — api.thanafit.com

**Status:** Documentation only — **no DNS changes made**  
**Production URL:** `http://api.thanafit.com/api`

---

## Current wiring (source account `114749311002`)

### Route 53 hosted zone

| Field | Value |
|-------|-------|
| Domain | `thanafit.com` |
| Hosted zone ID | `Z09775432VGRB7DELDK5Q` |
| Type | Public |
| Record count | 10 |
| Created via | Amplify request reference |

Zone lives in **source** AWS account (same account as HealthApp infra).

### api.thanafit.com record

| Field | Value |
|-------|-------|
| Name | `api.thanafit.com` |
| Type | **A (alias)** |
| Alias target | `dualstack.healthapp-alb-1571435665.us-east-1.elb.amazonaws.com` |
| ALB hosted zone ID | `Z35SXDOTRQ7X7K` |
| Evaluate target health | `true` |
| TTL | N/A (alias) |

### ACM validation (present, not in Terraform)

| Name | Type | Purpose |
|------|------|---------|
| `_e3cd79b2afcac4f46d6c717162a1de72.api.thanafit.com` | CNAME | ACM certificate validation |
| TTL | 300 | |

### DNS resolution verification

```
api.thanafit.com → 3.229.100.70, 54.166.175.90
healthapp-alb-1571435665.us-east-1.elb.amazonaws.com → same IPs
```

Confirmed: production API traffic flows **Route 53 → source ALB → ECS**.

---

## Target cutover (approval required)

**Prerequisites:**

1. Target ALB healthy: `http://<target-alb-dns>/api/actuator/health` returns 200
2. ECS tasks stable on target account
3. Explicit stakeholder approval for DNS change

### Cutover steps

1. **Pre-cutover:** Note current source ALB DNS and zone ID for rollback:
   - Source ALB: `healthapp-alb-1571435665.us-east-1.elb.amazonaws.com`
   - Source ALB zone: `Z35SXDOTRQ7X7K`

2. **Optional TTL reduction:** If any non-alias records exist with high TTL, lower before cutover (alias record has no TTL).

3. **Update Route 53 record** in hosted zone `Z09775432VGRB7DELDK5Q`:

   ```
   Record: api.thanafit.com
   Type: A (alias)
   Alias target: dualstack.<target-alb-dns>
   Target hosted zone ID: <target-alb-zone-id from terraform output>
   Evaluate target health: true
   ```

   Values from target Terraform outputs:
   - `alb_dns_name`
   - `alb_zone_id`

4. **Verify propagation:**
   ```bash
   dig +short api.thanafit.com
   curl -sf http://api.thanafit.com/api/actuator/health
   ```

5. **Monitor** 24–48 hours: ECS, RDS, ALB 5xx, CloudWatch logs.

### Cross-account DNS note

Route 53 zone remains in **source** account unless migrated separately. Cutover only changes the **alias target** to the target account ALB (cross-account alias is supported for ALB).

If the zone moves to target account later, export/import or recreate zone and update registrar NS records.

---

## HTTPS consideration

Current production uses **HTTP** (`http://api.thanafit.com/api`). No HTTPS listener in Terraform. Adding HTTPS requires ACM certificate + ALB:443 listener — separate change, not part of account migration unless requested.

---

## Rollback

See `08-rollback.md` — revert `api.thanafit.com` alias to source ALB. Source infra remains running.
