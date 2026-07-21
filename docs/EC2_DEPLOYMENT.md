# FabFlix — AWS EC2 Deployment Guide

This guide deploys the existing Docker Compose stack (`docker-compose.yml`: 2x Tomcat app nodes, Postgres primary/replica, 6-node Redis Cluster, Apache HTTP Server load balancer) onto a single AWS EC2 instance. This is the "lift and shift" path — no ECS/EKS, no ELB yet. It's the deployment target tracked as open work in [`remaining-steps.md`](remaining-steps.md).

A managed-Kubernetes deployment (EKS) is a separate, later step — the manifests already exist under [`../k8s/`](../k8s/) but are unapplied. An AWS ELB migration (replacing the `lb` Apache container) is also separate. Both are out of scope here.

---

## 1. Architecture on EC2

Everything in `docker-compose.yml` runs as containers on **one EC2 instance** via Docker Compose — this is not a multi-instance deployment. That means the "load balancer" and "replica" in the stack are for resilience against process/container failure and read-scaling, not for spreading load across hardware. Horizontal scaling across multiple EC2 instances is what the future ELB/EKS work is for.

```
Internet
   |
   v
EC2 Security Group (80, 443 open)
   |
   v
Apache LB (container, host port 80) --> app1 (Tomcat, 8080)
                                     --> app2 (Tomcat, 8080)
                                              |
                          +-------------------+-------------------+
                          v                                       v
                  db-primary (Postgres, 5432)        redis-node-1..6 (Cluster, 6379)
                          |
                          v
                  db-replica (Postgres, 5433, streaming replication)
```

## 2. Prerequisites

- An AWS account with permission to create EC2 instances, security groups, key pairs, and (optionally) Elastic IPs.
- `aws` CLI configured locally (`aws configure`), or use the AWS Console — steps below show both where they diverge.
- A TMDB API key and a Google reCAPTCHA site/secret key pair for production (see main [`README.md`](../README.md)).
- This repo pushed to a Git remote the EC2 instance can pull from (GitHub, etc.), or an artifact registry — this guide assumes `git clone` on the instance.

## 3. Choose an instance size

The stack runs **11 containers** (2 app, 2 Postgres, 6 Redis, 1 Redis cluster-init, 1 Apache LB). Free Tier `t2.micro`/`t3.micro` (1 vCPU, 1 GB RAM) is **not enough** — the JVMs alone (2x Tomcat) plus 6 Redis nodes will OOM or swap heavily.

| Instance type | vCPU | RAM | Notes |
|---|---|---|---|
| `t3.micro` (Free Tier) | 2 | 1 GB | Not recommended — will struggle to keep 11 containers up |
| `t3.small` | 2 | 2 GB | Minimum workable size for demo/low-traffic use |
| `t3.medium` | 2 | 4 GB | **Recommended** — comfortable headroom for JVM heaps + Redis + Postgres |

Storage: 20–30 GB `gp3` EBS volume (default 8 GB root volume is too small once Docker images, Postgres data, and Redis data accumulate).

## 4. Launch the EC2 instance

### Console
1. EC2 → Launch Instance.
2. AMI: **Amazon Linux 2023** (or Ubuntu 22.04 — commands below cover both).
3. Instance type: `t3.medium` (see sizing above).
4. Key pair: create or select one; you'll need the `.pem` to SSH in.
5. Network settings → create a new security group with:
   - SSH (22) from **your IP only** (not `0.0.0.0/0`)
   - HTTP (80) from `0.0.0.0/0`
   - HTTPS (443) from `0.0.0.0/0` — only if/when you terminate TLS on this box directly
6. Storage: 30 GB `gp3`.
7. Launch.

### CLI equivalent
```bash
aws ec2 create-security-group \
  --group-name fabflix-sg \
  --description "FabFlix EC2 security group"

MY_IP=$(curl -s https://checkip.amazonaws.com)
aws ec2 authorize-security-group-ingress --group-name fabflix-sg \
  --protocol tcp --port 22 --cidr "${MY_IP}/32"
aws ec2 authorize-security-group-ingress --group-name fabflix-sg \
  --protocol tcp --port 80 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-name fabflix-sg \
  --protocol tcp --port 443 --cidr 0.0.0.0/0

aws ec2 run-instances \
  --image-id ami-xxxxxxxxxxxxxxxxx \
  --instance-type t3.medium \
  --key-name your-key-pair \
  --security-groups fabflix-sg \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=fabflix-app}]'
```

(Optional) Allocate and associate an Elastic IP so the public address survives instance stop/start:
```bash
aws ec2 allocate-address --domain vpc
aws ec2 associate-address --instance-id i-xxxxxxxxxxxxxxxxx --allocation-id eipalloc-xxxxxxxxxxxxxxxxx
```

## 5. Install Docker + Docker Compose on the instance

SSH in first:
```bash
ssh -i your-key-pair.pem ec2-user@<PUBLIC_IP>   # Amazon Linux
# or: ssh -i your-key-pair.pem ubuntu@<PUBLIC_IP>   # Ubuntu
```

**Amazon Linux 2023:**
```bash
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker   # or log out/in

DOCKER_COMPOSE_VERSION=v2.29.7
sudo curl -SL "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose version
```

**Ubuntu 22.04:**
```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker
docker compose version   # note: 'docker compose', no hyphen, on this package
```

> Adjust `docker-compose` vs `docker compose` in the commands below to match whichever variant your distro installed.

## 6. Clone the repo and configure secrets

```bash
git clone <your-repo-url> fabflix
cd fabflix
```

The stack reads `TMDB_API_KEY` and `RECAPTCHA_SECRET` from a `.env` file next to `docker-compose.yml` (gitignored — never commit it). Create it on the instance:

```bash
cat > .env <<'EOF'
TMDB_API_KEY=your_real_tmdb_api_key
RECAPTCHA_SECRET=your_real_recaptcha_secret_key
EOF
chmod 600 .env
```

Get a TMDB key at https://www.themoviedb.org/settings/api and a reCAPTCHA key pair at https://www.google.com/recaptcha/admin — do **not** ship to production with Google's public test keys (`6LeIxAcTAAAA...`), which accept every request and defeat the CSRF/bot protection this app relies on. Also update the reCAPTCHA **site key** referenced by the frontend (`src/main/resources/config.properties` → `recaptcha.site.key`) to match, and rebuild the image, since the site key is baked into the served pages rather than read from `.env`.

Also change the default DB/Postgres passwords baked into `docker-compose.yml` (`POSTGRES_PASSWORD: password`, `DB_PASSWORD: password` under `app1`/`app2`) before exposing this instance publicly — see [Section 10](#10-hardening-before-going-live).

## 7. Build and start the stack

```bash
docker-compose up --build -d
docker-compose ps
```

First boot takes a few minutes: Maven build inside the `Dockerfile`'s build stage, then Postgres primary init (loads `sql/*.sql` via `docker-entrypoint-initdb.d`), then replica streaming setup, then Redis cluster formation (`redis-cluster-init` runs once and exits — that's expected, check its logs if the app can't reach Redis).

Known pitfall already hit and documented in [`remaining-steps.md`](remaining-steps.md): `sql/00_configure_hba.sh` must be executable (`chmod +x sql/00_configure_hba.sh`) or `db-replica` will crash-loop on a fresh volume because `pg_hba.conf` never gets the replication grant. Verify before first boot:
```bash
git ls-files -s sql/00_configure_hba.sh   # mode should start with 100755, not 100644
```

Watch logs while it comes up:
```bash
docker-compose logs -f app1 app2 db-primary db-replica redis-cluster-init
```

## 8. Verify the deployment

```bash
curl -s http://localhost/fabflix/api/health
# {"status":"UP"}
```

From your own machine:
```bash
curl -s http://<PUBLIC_IP>/fabflix/api/health
```

Then open `http://<PUBLIC_IP>/fabflix/` in a browser and walk through the manual QA checklist in [`final-test.md`](final-test.md) (auth, cart, checkout, admin dashboard, etc.) before calling the deployment done.

Check that both app nodes are actually receiving traffic through the LB (session affinity is configured via `JVM_ROUTE`/`jvmRoute` in `docker-compose.yml` + `conf/httpd.conf`):
```bash
docker-compose logs lb | grep -i "app1\|app2" | tail -20
```

## 9. DNS and HTTPS

The Apache LB container listens on host port 80 only; `docker-compose.yml` doesn't map 443. Two options:

**Option A — TLS at the app layer (matches current Dockerfile):** The `Dockerfile` already generates a self-signed cert and configures Tomcat for HTTPS on 8443 inside each app container (see `conf/server.xml`). To expose it, add a `443:8443` (or similar) port mapping and route TLS traffic to `app1`/`app2` directly instead of through the plain-HTTP Apache LB, or add TLS termination to `conf/httpd.conf` and map port 443 on the `lb` service. A self-signed cert will show browser warnings — fine for a demo, not for real users.

**Option B — TLS via Let's Encrypt on the LB (recommended for anything public-facing):** Add `certbot`, obtain a cert for your domain, and terminate TLS in the `lb` Apache container (`mod_ssl` is not currently loaded in `conf/httpd.conf` — you'll need to add it), proxying plaintext to `app1`/`app2` same as today. This requires a real domain pointed at the instance's IP (or Elastic IP) via an A record, since Let's Encrypt validates domain ownership.

Either way, point a DNS A record at the instance's Elastic IP so the address survives restarts and so you have a stable name for the TLS cert.

## 10. Hardening before going live

- **Change default passwords.** `docker-compose.yml` hardcodes `POSTGRES_PASSWORD: password` and `DB_PASSWORD: password` for `app1`/`app2`. Move these into `.env` and reference via `${...}` the same way `TMDB_API_KEY` already is, so they're not sitting in plaintext in version control.
- **Swap the reCAPTCHA test keys** (see Section 6) — this is the single most important item; the test secret accepts any token and silently disables the anti-bot/CSRF-adjacent protection on login/registration.
- **Restrict SSH** to your IP (done at launch above) — don't widen it later.
- **Don't expose Postgres/Redis ports externally.** `docker-compose.yml` maps `5432` and `5433` to the host (useful for local debugging); on a public EC2 instance, either remove those port mappings or restrict them in the security group so only your IP can reach them, since they're only needed by the containers on the internal Docker network.
- **Set up automated backups.** Postgres data lives in the `postgres-primary-data` named volume — nothing currently backs it up. At minimum, cron a `pg_dump` to S3.
- **Turn on EC2 instance monitoring / a swap file** if you undersize the instance — Java + Redis workloads are memory-hungry and a t3.small will need swap to avoid OOM-killed containers under load.

## 11. Day-2 operations

**Deploy a new version:**
```bash
cd fabflix
git pull
docker-compose up --build -d   # rebuilds changed images, restarts affected containers only
```

**Restart everything cleanly:**
```bash
docker-compose down
docker-compose up -d
```

**Wipe and reseed the database** (destructive — only for resetting a demo/test environment):
```bash
docker-compose down -v
docker-compose up --build -d
```

**Tail logs:**
```bash
docker-compose logs -f app1
```

**Check container health:**
```bash
docker-compose ps
```

## 12. Estimated cost

Rough us-east-1 on-demand pricing, for budgeting purposes only — check the [AWS Pricing Calculator](https://calculator.aws) for current rates:

| Item | Approx. monthly cost |
|---|---|
| `t3.medium` on-demand, 24/7 | ~$30 |
| 30 GB `gp3` EBS | ~$2.50 |
| Elastic IP (attached to a running instance) | $0 |
| Data transfer out | Usage-dependent, first 100 GB/mo free |

A `t3.small` (~$15/mo) is viable for a low-traffic demo if you accept less headroom; see Section 3.

---

## Next steps beyond this guide

Per [`remaining-steps.md`](remaining-steps.md), the follow-on production-readiness work not covered here:
- **AWS Elastic Load Balancer** — replace the Apache `lb` container with a real ALB once running multiple EC2 instances (or moving to ECS/EKS) makes a software LB on a single box insufficient.
- **Kubernetes (EKS/GKE)** — the manifests in [`../k8s/`](../k8s/) (StatefulSets for Postgres and Redis Cluster, `hpa.yaml`, `network-policy.yaml`) already model a production-grade topology; they just haven't been applied to a managed cluster yet.
