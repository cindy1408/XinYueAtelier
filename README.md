# XinYue Atelier

A full-stack sewing pattern library. Sign in with Google, organise patterns in nested folders, upload PDFs, and preview or download them from anywhere.

**Stack:** React 19 + Vite · Spring Boot 3.5 (Java 25) · PostgreSQL 16 + Flyway · S3 for files · Google OAuth2 + server-side sessions · Terraform on AWS

---

## Where we're at

| Area | State |
|---|---|
| App features (login, folders, PDF upload/preview/download/delete) | Done |
| Local dev (Docker Compose + LocalStack) | Done |
| Prod on AWS (ECS Fargate, RDS, ALB, ACM, Route 53, S3, Secrets Manager) | Live, but costs roughly £50/month |
| Tests and checks (`make ci`: Checkstyle, JaCoCo, ESLint, Vitest) | Done |
| Terraform for backend infra | Done (separate `dev` and `prod` environments) |
| Frontend hosting (S3 + CloudFront + Route 53) | Done in Terraform (`environments/prod`). The WAF web ACL and the CloudFront ACM certificate were created by hand and are referenced by ARN |
| CI/CD pipeline | Not yet. Deploys are manual scripts |
| Cheaper personal deployment (single host) | **Planned**, see below |

### Current priority

Cut the running cost of my personal copy. The app is fine; the infrastructure is sized for many users and I'm the only one using it.

### Where the money goes (estimates, check current AWS pricing)

| Component | Approx. monthly |
|---|---|
| ECS Fargate (1 task) | $15–30 |
| RDS PostgreSQL (small instance) | $10–15 |
| Application Load Balancer | ~$16 |
| S3 storage | ~$0.02–0.03 per GB, so pennies for personal use |
| CloudFront WAF web ACL (created by hand) | ~$5+ |
| Misc (CloudWatch, etc.) | $5–10 |

S3 is not the cost problem. Compute, the database and the load balancer are.

---

## Two deployment profiles

The same app code supports both. Only the infrastructure differs.

### A. AWS ECS reference architecture (current prod)

Production-style setup: managed, highly available, scalable in principle.

- Backend on ECS Fargate behind an ALB with ACM-managed HTTPS
- RDS Postgres with deletion protection (prod)
- Files in S3, served via presigned URLs
- Separate ephemeral `dev` environment that can be created and destroyed on demand
- Kept as a reference and demo. It can be brought up again with `terraform apply` when needed.

### B. Single host (planned, for personal use)

One small server running the same stack with Docker Compose.

- App, Postgres and a reverse proxy (e.g. Caddy for automatic HTTPS) on one box
- Files stay in S3 (cheap)
- Target cost: roughly £5–12/month

**Trade-offs accepted:** no managed DB backups (replace with scheduled `pg_dump` to S3), brief downtime on deploys, single point of failure, and OS patching is my job. On a non-AWS host there is no task role, so S3 access needs a tightly scoped IAM user. An EC2 instance with an instance role avoids that.

---

## Next steps

Moving the personal copy to a single host:

- [ ] Tag the current commit `aws-ecs-reference` so the AWS version is frozen as-is
- [ ] Choose the host (EC2 with instance role vs a budget VPS)
- [ ] Add a production Compose file and reverse-proxy config (backend, Postgres, proxy)
- [ ] Add nightly `pg_dump` to S3 and **test a restore**
- [ ] Dump the RDS database and restore it on the new host; check folders and patterns load
- [ ] Confirm the S3 bucket and presigned URLs work from the new host
- [ ] Update Google OAuth redirect URIs if the API domain or host changes
- [ ] Lower DNS TTL, then repoint the API record in Route 53 to the new host
- [ ] Take a final RDS snapshot, disable deletion protection, and destroy the ECS/ALB/RDS resources
- [ ] Move the current Terraform into `infra/aws-ecs/` and add `infra/single-host/`

Later, as time allows:

- [ ] Bring the WAF and CloudFront certificate under Terraform, and move bootstrap resources (state bucket, lock table, ECR) into their own stack
- [ ] Add a deploy pipeline (e.g. GitHub Actions)
- [ ] Add backups/alerts to whichever profile is live
- [ ] Optional: store files in the user's own Google Drive instead of S3

---

## Known gaps in the AWS profile

These are the honest reasons it is "production-style", not fully production-grade:

- Fargate tasks use public IPs on the default VPC (no private subnets/NAT or VPC endpoints)
- Single task, no autoscaling. Sessions are in memory, so running more than one task needs sticky sessions or a shared session store (e.g. Spring Session)
- Cookie auth uses server-side sessions, CSRF tokens on unsafe requests, and `Secure; SameSite=Lax` in production
- Uploads are read fully into memory (`byte[]`) before going to S3, so memory use scales with file size (max 50 MB)
- Task size is 0.25 vCPU / 512 MB for a Java 25 app, which is tight
- ALB uses the old `ELBSecurityPolicy-2016-08` TLS policy
- Debug logging for Spring Security is on in prod
- Some values are hard-coded in the module (CORS origins, OAuth redirect URI, CloudFront aliases), so a second environment on another domain needs them parameterised
- Manual deploys, no pipeline
- No monitoring or alarms
- Database backup and restore process not rehearsed
- IAM policies could be tighter

---

## Architecture

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, served via Nginx (local) / S3 + CloudFront (prod) |
| Backend | Java 25, Spring Boot 3.5 |
| Database | PostgreSQL 16, JPA, Flyway |
| File storage | S3 (LocalStack locally) |
| Auth | Google OAuth2 login; server-side session cookie (`JSESSIONID`, `Secure`, `SameSite=Lax` in prod). Users are linked by the Google `sub` claim. Sessions are held in memory |
| Local orchestration | Docker Compose |
| Prod infra | Terraform (ECS Fargate, RDS, ALB, ACM, Route 53, ECR, Secrets Manager, IAM, CloudWatch) |

---

## Local development

### 1. Create `.env` in the repo root

```dotenv
JWT_SECRET=replace-with-a-long-random-value
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
ALLOWED_GOOGLE_EMAIL=you@example.com
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
APP_FRONTEND_URL=http://localhost:5173
S3_BUCKET=xin-yue-atelier
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_REGION=eu-west-2
VITE_API_URL=http://localhost:8080
```

In Google Cloud Console, add `http://localhost:8080/login/oauth2/code/google` as an authorised redirect URI.
Only the Google account matching `ALLOWED_GOOGLE_EMAIL` can use this personal instance.

### 2. Start everything

```bash
docker compose up --build
```

| What | Where |
|---|---|
| Frontend | http://localhost:5173 |
| Backend health | http://localhost:8080/actuator/health |
| Postgres (from host) | `localhost:5433`, db `atelierdb`, user `atelieruser` |
| LocalStack S3 | http://localhost:4566 |

Useful:

```bash
docker compose logs -f backend
docker compose up --build backend      # rebuild one service
docker compose exec localstack awslocal s3 ls s3://xin-yue-atelier --recursive
docker compose down                    # stop, keep data
docker compose down -v                 # stop and WIPE local data (careful)
```

### Running without Docker

```bash
cd atelier-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd atelier-frontend && npm ci && npm run dev
```

When running Spring Boot directly with the local profile, it connects to the Compose Postgres at `localhost:5433`. When running the backend inside Compose, the container uses `db:5432`.

---

## Deploying (current AWS prod)

Step-by-step commands, dev environment instructions and debugging tips live in **[RUNBOOK.md](RUNBOOK.md)**. The short version:

```bash
./deploy-backend-prod.sh     # build amd64 image, push to ECR, force new deployment, wait, health check
./deploy-frontend-prod.sh    # build, sync to the frontend bucket, invalidate CloudFront
```

- Always build images with `--platform linux/amd64` on Apple Silicon (the scripts already do).
- Pushing to `:latest` alone does not redeploy; the scripts force a new deployment.
- Infra changes: `cd terraform/environments/prod`, run `terraform plan`, read it fully, then `terraform apply`.

---

## Checks

```bash
make ci
```

Runs backend Checkstyle, Maven verify and JaCoCo, then frontend install, ESLint, Vitest coverage and the production build.

---

## API summary

**Folders**
- `GET /folder`: list root folders
- `GET /folder/{folderId}`: get a folder
- `GET /folder/{parentId}/children`: list child folders
- `POST /folder`, `POST /folder/{parentId}`: create folders
- `PUT /folder/{id}`: update a folder
- `DELETE /folder/{id}`: delete a folder, its descendants and their patterns

**Patterns**
- `POST /patterns/{folderId}`: upload a PDF
- `GET /patterns/{folderId}/files`: list patterns
- `DELETE /patterns/{patternId}`: delete a pattern
- `GET /patterns/download/{patternId}`: download a pattern
- `GET /patterns/preview/{patternId}`: get a preview URL

---

## Security notes

- Google sign-in is restricted to `ALLOWED_GOOGLE_EMAIL`; set the required `allowed_google_email` Terraform input for each AWS environment.
- Never commit `.env`, `terraform.tfvars`, `tfplan`, Terraform state, OAuth client secrets or AWS keys.
- `application-secrets.properties` lives in `src/main/resources`, so it is packaged into the jar and the Docker image. Move it out of `resources/`, add a `.dockerignore`, and make local config use environment variables only.
- Delete the stale root-level `terraform.tfstate` and `terraform.tfstate.backup`, and the saved `tfplan`.
- Rotate any credential that was ever stored in `application-secrets.properties`, and remove it from git history before making the repo public.
- Before sharing publicly, replace hard-coded AWS account IDs, bucket names and CloudFront distribution IDs in the scripts and RUNBOOK with variables.
- Prefer IAM roles (ECS task role or EC2 instance role) over long-lived access keys.