# FabFlix — AWS EC2 Deployment Guide

This guide deploys the existing Docker Compose stack (`docker-compose.yml`: 2x Tomcat app nodes, Postgres primary/replica, 6-node Redis Cluster, Apache HTTP Server load balancer) onto a single AWS EC2 instance. This is the "lift and shift" path — no ECS/EKS, no ELB yet. It's the deployment target tracked as open work in [`remaining-steps.md`](remaining-steps.md).

A managed-Kubernetes deployment (EKS) is a separate, later step — the manifests already exist under [`../k8s/`](../k8s/) but are unapplied. An AWS ELB migration (replacing the `lb` Apache container) is also separate. Both are out of scope here.

**Two deployment paths in this guide:**
- **Sections 1–12** — the full HA stack (2 app nodes, DB replica, 6-node Redis Cluster, LB) on a `t3.small`/`t3.medium`. This is **not free-tier eligible**.
- **[Section 13](#13-free-tier-deployment-t3microt2micro-1-gb-ram)** — a stripped-down variant (1 app node, single-node Redis "cluster," no DB replica, no LB) sized to actually run on a free-tier-eligible `t3.micro`/`t2.micro` (1 GB RAM), using a separate `docker-compose.free-tier.yml`. Read this section first if cost is the priority — it changes several steps below.

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

The full stack in `docker-compose.yml` runs **11 containers** (2 app, 2 Postgres, 6 Redis, 1 Redis cluster-init, 1 Apache LB). Free Tier `t2.micro`/`t3.micro` (1 vCPU, 1 GB RAM) is **not enough for that stack** — the JVMs alone (2x Tomcat) plus 6 Redis nodes will OOM or swap heavily.

| Instance type | vCPU | RAM | Notes |
|---|---|---|---|
| `t3.micro`/`t2.micro` (Free Tier) | 1–2 | 1 GB | Not enough for the full 11-container stack. **Usable only with the stripped-down [free-tier variant](#13-free-tier-deployment-t3microt2micro-1-gb-ram)** (1 app node, 1 Redis node, no replica, no LB). |
| `t3.small` | 2 | 2 GB | Minimum workable size for the full stack, demo/low-traffic use. Not free-tier eligible (~$15/mo). |
| `t3.medium` | 2 | 4 GB | **Recommended** for the full stack — comfortable headroom for JVM heaps + Redis + Postgres. Not free-tier eligible (~$30/mo). |

Storage: 20–30 GB `gp3` EBS volume (default 8 GB root volume is too small once Docker images, Postgres data, and Redis data accumulate).

> **Note on "Free Tier" itself:** the 750 hrs/mo of free `t2.micro`/`t3.micro` compute only applies to AWS accounts created **before 2025-07-15**, for 12 months from account creation. Accounts created on or after that date get a $200 promotional credit pool instead (typically valid ~6–12 months) — there's no longer an ongoing free instance-hour allowance for new accounts. Either way, EBS free tier (30 GB) is only reliably guaranteed for `gp2`/magnetic volumes, not `gp3` — see [Section 13](#13-free-tier-deployment-t3microt2micro-1-gb-ram) for the storage type to use if you want to stay inside that guarantee.

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
| `t3.micro`/`t2.micro`, free-tier variant ([Section 13](#13-free-tier-deployment-t3microt2micro-1-gb-ram)) | $0 if within 750 free hrs/mo on a legacy (pre-2025-07-15) account; otherwise standard on-demand rate (~$6–8/mo) or drawn from promotional credit |

A `t3.small` (~$15/mo) is viable for a low-traffic demo of the full stack if you accept less headroom; see Section 3. For an actually-free option, use the [free-tier variant](#13-free-tier-deployment-t3microt2micro-1-gb-ram) instead — it trades away HA (no DB replica, no Redis cluster resilience, single app node, no LB) to fit in 1 GB of RAM.

---

## 13. Free tier deployment (t3.micro/t2.micro, 1 GB RAM)

This variant fits inside a genuinely free-tier-eligible instance by cutting the stack down to what 1 GB of RAM can actually hold: **1 app node, 1 Postgres node (no replica), 1 Redis node (no cluster resilience), no separate Apache LB.** It uses a dedicated compose file, `docker-compose.free-tier.yml`, instead of `docker-compose.yml` — the two are independent; nothing here modifies the full HA stack described in Sections 1–12.

### 13.1 What's different from the full stack

```
Internet
   |
   v
EC2 Security Group (80, 443 open)
   |
   v
app1 (Tomcat, host ports 80->8080, 443->8443)
        |
        +----------------+
        v                v
db-primary (Postgres)   redis-node-1 (single-node "cluster", 0 replicas)
```

- **No DB replica** — `app1` reads and writes through `db-primary`. `RedisUtil`/JDBC don't require a replica to function; you just lose read-scaling and the failover story.
- **Redis is 1 node, not 6** — `redis-node-1` is still put into cluster mode (`cluster-enabled yes` in `conf/redis-cluster.conf`) so `RedisUtil`'s `JedisCluster` client works unmodified, but `redis-cli --cluster create` refuses fewer than 3 master nodes. `docker-compose.free-tier.yml` works around this with a `redis-cluster-init` step that runs `CLUSTER ADDSLOTSRANGE 0 16383` directly on the single node instead. If this node goes down, `RedisUtil` catches the failure and disables caching/rate-limiting/distributed sessions rather than crashing the app (see the `redisEnabled` fallback in `RedisUtil.java`) — degraded, not down.
- **No LB, no second app node** — `app1`'s Tomcat container is mapped straight to host ports 80/443. There's nothing to load-balance across with only one node, so `JVM_ROUTE`/session affinity and the `lb` Apache container are dropped entirely.
- **JVM heap is capped explicitly** — `CATALINA_OPTS` sets `-Xmx320m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC` instead of letting the JVM's default ergonomics (usually ~25% of visible host RAM) fight the other containers for memory.
- **Postgres is tuned down** — `shared_buffers=32MB`, `max_connections=20` instead of the image defaults (128 MB buffers), and every service has an explicit `mem_limit` so one container can't starve the others.

### 13.2 Launch the instance

Same as [Section 4](#4-launch-the-ec2-instance), with these substitutions:
- Instance type: `t3.micro` (or `t2.micro`, whichever your region/account offers as the free-tier option — see the note in [Section 3](#3-choose-an-instance-size) about account-age eligibility).
- Storage: use **`gp2`**, not `gp3` — the EBS free tier (30 GB) is only reliably documented as covering `gp2`/magnetic. 15–20 GB is enough for this smaller stack; staying well under 30 GB total account-wide (across all your volumes/snapshots) keeps you inside the free allowance.
- Everything else (security group, key pair, AMI choice) is unchanged.

### 13.3 Create a swap file before building anything

This is the step people skip and then can't figure out why `docker-compose up --build` hangs or the instance becomes unresponsive: **the Maven build stage in `Dockerfile` (`mvn package`) itself needs several hundred MB of heap**, and on a fresh 1 GB instance that alone can OOM before any of the runtime containers even start. Set up swap first:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

With swap in place the build will succeed but will be slow (swapping to disk). If you expect to redeploy often, it's faster to build the image elsewhere (locally, or in CI) and `docker push` it to a registry, then change `docker-compose.free-tier.yml`'s `app1.build: .` to `app1.image: <your-registry>/fabflix:latest` and `docker pull` on the instance instead of building in place.

### 13.4 Install Docker, clone, configure secrets

Same as [Sections 5–6](#5-install-docker--docker-compose-on-the-instance) — no differences.

### 13.5 Build and start the stack

```bash
docker-compose -f docker-compose.free-tier.yml up --build -d
docker-compose -f docker-compose.free-tier.yml ps
```

Watch logs while it comes up (expect this to take longer than the full-stack build on a `t3.medium`, since you're likely swapping):
```bash
docker-compose -f docker-compose.free-tier.yml logs -f app1 db-primary redis-cluster-init
```

### 13.6 Verify

```bash
curl -s http://localhost/fabflix/api/health
```

Then walk the manual QA checklist in [`final-test.md`](final-test.md) same as [Section 8](#8-verify-the-deployment). There's no `lb` container to check session affinity against — there's only one app node.

### 13.7 Tradeoffs to accept

- **No failover.** One app node, one DB node, one Redis node — any of the three going down takes that piece of functionality out (Redis degrades gracefully per above; Postgres or Tomcat going down takes the app down).
- **Deploys cause a brief outage.** `docker-compose -f docker-compose.free-tier.yml up --build -d` recreates `app1` in place — there's no second node to drain traffic to first.
- **Headroom is thin.** 1 GB total RAM with `mem_limit`s totaling ~640 MB across containers leaves relatively little for the OS/Docker daemon; watch `docker stats` and `free -h` under real traffic, and treat the swap file as a safety net, not a performance feature.
- Hardening steps in [Section 10](#10-hardening-before-going-live) (default passwords, reCAPTCHA test keys, restricting Postgres/Redis ports) still apply — the free-tier compose file inherits the same default credentials from `docker-compose.yml` and needs the same treatment before going public.

---

## Next steps beyond this guide

Per [`remaining-steps.md`](remaining-steps.md), the follow-on production-readiness work not covered here:
- **AWS Elastic Load Balancer** — replace the Apache `lb` container with a real ALB once running multiple EC2 instances (or moving to ECS/EKS) makes a software LB on a single box insufficient.
- **Kubernetes (EKS/GKE)** — the manifests in [`../k8s/`](../k8s/) (StatefulSets for Postgres and Redis Cluster, `hpa.yaml`, `network-policy.yaml`) already model a production-grade topology; they just haven't been applied to a managed cluster yet.
