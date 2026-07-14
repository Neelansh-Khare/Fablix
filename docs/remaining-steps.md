# FabFlix Remaining Steps (2026-07-13)

This doc summarizes what's left across `final-test.md` (manual QA checklist) and `nextStepsV2.md` (roadmap), based on the current state of the `v4` branch.

---

## 1. Manual QA Pass (`docs/final-test.md`)

Local stack is now built and running (`docker-compose up --build -d`), with `.env` configured and a stale-volume replication bug fixed (see [Notes](#notes) below). None of the checklist has been walked through yet. Still to verify:

- [ ] **Auth** — registration, login/logout, password validation, CSRF token issuance, wrong-password handling
- [ ] **Home page** — movie grid loads, posters render, movie cards link to detail pages
- [ ] **Search & autocomplete** — suggestions dropdown, movie/star results, empty-search handling
- [ ] **Browse** — genre/letter filters, pagination
- [ ] **Movie detail page** — metadata, star links, add-to-cart, recommendations section
- [ ] **Cart** — quantity controls, item removal, totals, persistence, logged-out redirect
- [ ] **Checkout** — form validation, order completion, cart clears, order history updates
- [ ] **Profile** — edit name/address, change password, order history
- [ ] **CSRF protection** — unauthenticated POST to `/fabflix/api/cart` returns 403
- [ ] **Rate limiting** — 6 failed logins triggers account lockout message
- [ ] **Admin dashboard** — analytics view, Add Movie / Add Star forms, poster/TMDB status
- [ ] **Error handling** — invalid movie ID, 404 page, no stack traces leaked in responses

## 2. Production Readiness (`docs/nextStepsV2.md`, section 5.2/6.2)

Everything else in the roadmap is complete. Remaining:

- [ ] **EC2 Deployment** — deploy the Docker Compose stack to AWS EC2 (Free Tier)
- [ ] **AWS Elastic Load Balancer** — migrate off the current Apache HTTP Server software load balancer to AWS ELB
- [ ] **Kubernetes deployment** — deploy to a managed cluster (EKS/GKE). The production-grade StatefulSet manifests for Postgres and Redis Cluster already exist; they just haven't been applied to a real cluster.

---

## Notes

- `.env` was missing on this machine; created with `TMDB_API_KEY` and the standard local-dev `RECAPTCHA_SECRET` test key from `final-test.md`.
- Found and fixed a real repo bug: `sql/00_configure_hba.sh` was committed without the executable bit (`100644` instead of `100755`). Postgres's docker-entrypoint init runner silently failed with `Permission denied` on it, so `pg_hba.conf` never got the replication grant for the `replicator` user, and `db-replica` crash-looped on *any* fresh volume (not just this machine). Fixed via `chmod +x sql/00_configure_hba.sh` — **this fix is uncommitted**, should be committed so new clones/CI don't hit the same failure.
- After the fix + `docker-compose down -v && docker-compose up --build -d`, all 11 containers come up healthy, `db-replica` streams WAL correctly, and `GET /fabflix/api/health` returns `{"status":"UP"}`.
