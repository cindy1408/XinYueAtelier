# XinYue Atelier
XinYue Atelier is a full-stack sewing pattern library. Users authenticate with Google, organise patterns in a hierarchy of folders, upload PDF files to S3, and download or preview those files through presigned URLs.

## Project Status

### Implemented

- React/Vite frontend with login, protected routes, folder navigation, folder creation/editing/deletion, and pattern upload/delete/preview flows.
- Spring Boot backend with Google OAuth2 login, authenticated sessions/JWT API requests, CORS configuration, folder and pattern APIs, validation, and an actuator health endpoint.
- PostgreSQL persistence with Flyway migrations for users, folders, and patterns.
- S3-backed PDF storage and presigned preview/download URLs.
- Docker Compose development environment containing PostgreSQL, Spring Boot, LocalStack S3, and an Nginx-served frontend.
- Terraform resources for an AWS production foundation: ECR, ECS Fargate, RDS PostgreSQL, S3, Secrets Manager, IAM, CloudWatch logs, an HTTPS ALB, ACM certificate validation, and Route 53 API DNS.
- Backend and frontend unit/controller/service tests, Checkstyle, ESLint, JaCoCo, and frontend coverage scripts.

### Still needed or worth checking

- Rotate credentials in local or historical `application-secrets.properties` file. use environment variables or AWS Secrets Manager.
- Add a Terraform-managed frontend hosting path. The current Terraform directory provisions the backend API infrastructure, not the frontend bucket or CloudFront distribution.
- Add a production deployment pipeline. At present, the backend image push and frontend publish are manual.
- Use private subnets/NAT or VPC endpoints for production instead of relying on public Fargate IPs. The current ECS service uses the default VPC subnets and `assign_public_ip = true`.
- Add database backups, deletion protection, tighter IAM policies, monitoring/alerts, and a production rollback strategy before treating the deployment as production-ready.
- Run the full CI checks after the current uncommitted backend and Terraform changes are settled.

## Architecture

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, Nginx |
| Backend | Java 25, Spring Boot 3.5 |
| Database | PostgreSQL 16, JPA, Flyway |
| File storage | AWS S3 or LocalStack S3 locally |
| Authentication | Google OAuth2 with authenticated sessions/JWT API requests |
| Local orchestration | Docker Compose |
| Production | ECS Fargate, RDS, ALB, ACM, Route 53, ECR |

## Prerequisites

- Docker Desktop with Compose
- Java 25 and Maven, only if running the backend outside Docker
- Node.js and npm, only if running the frontend outside Docker
- An AWS account and AWS CLI for production
- A Google OAuth application
- Terraform, version compatible with the AWS provider 5.x

## Local Development

### 1. Configure local secrets

Create `.env` in the repository root. The values below are examples; keep the file private.

```dotenv
JWT_SECRET=replace-with-a-long-random-value
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
APP_FRONTEND_URL=http://localhost:5173
S3_BUCKET=xin-yue-atelier
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_REGION=eu-west-2
VITE_API_URL=http://localhost:8080
```

In Google Cloud Console, add this authorized redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

The Compose file supplies the database connection to the backend. LocalStack creates the S3 bucket and applies `init/cors-config.json` when it becomes ready.

### 2. Start the complete local stack

From the repository root:

```bash
docker compose up --build
```

Open:

- Frontend: http://localhost:5173
- Backend health: http://localhost:8080/actuator/health
- PostgreSQL from the host: `localhost:5433`
- LocalStack S3 endpoint from the host: `http://localhost:4566`

Useful commands:

```bash
docker compose logs -f backend
docker compose exec localstack awslocal s3 ls s3://xin-yue-atelier --recursive
docker compose down
docker compose down -v  # also deletes local database and LocalStack data
```

### Optional: run services without Docker

Start PostgreSQL and LocalStack separately, then run the backend and frontend from their directories:

```bash
cd atelier-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

In another terminal:

```bash
cd atelier-frontend
npm ci
npm run dev
```

When the backend runs directly on the host, its local profile expects PostgreSQL at `localhost:5432`; update the local database port or connection settings if your PostgreSQL is exposed on another port.

## Production Deployment

The current production path deploys the API to ECS Fargate behind `https://api.xyatelier.com`. The frontend is built separately and published to the existing frontend hosting setup; frontend hosting is not currently represented in `terraform/`.

### 1. Configure AWS and Google OAuth

```bash
aws configure
aws sts get-caller-identity
aws ec2 create-default-vpc --region eu-west-2  # only if the account has no default VPC
```

In Google Cloud Console, add:

```text
https://api.xyatelier.com/login/oauth2/code/google
```

Confirm that `xyatelier.com` is hosted in the Route 53 account used by Terraform and that the ACM DNS validation records can be created there.

### 2. Bootstrap Terraform state

The Terraform backend uses the S3 bucket `atelier-terraform-state` and DynamoDB table `atelier-terraform-lock`. These must exist before `terraform init` can use the backend. Create them once with the AWS CLI, or temporarily remove/comment the backend block and apply only the state bucket/table resources, then restore the backend block and initialise.

The state bucket should have versioning and encryption enabled. Do not commit `terraform.tfvars` or state files.

### 3. Build and push the backend image

Terraform creates the ECR repository, so apply the repository first if it does not exist. From the repository root:

```bash
cd terraform
terraform init
terraform plan -target=aws_ecr_repository.backend
terraform apply -target=aws_ecr_repository.backend
ECR_REPOSITORY=$(terraform output -raw ecr_repository_url)
cd ../atelier-backend
aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin "$ECR_REPOSITORY"
docker buildx build --platform linux/amd64 --load -t atelier-backend:latest .
docker tag atelier-backend:latest "$ECR_REPOSITORY:latest"
docker push "$ECR_REPOSITORY:latest"
cd ../terraform
```

The `linux/amd64` image is required by the current ECS runtime image configuration.

### 4. Apply the AWS infrastructure

Create a private `terraform.tfvars` file with values for every required variable:

```hcl
aws_region           = "eu-west-2"
db_username          = "atelieruser"
db_password          = "replace-with-a-database-password"
google_client_id     = "your-google-client-id"
google_client_secret = "your-google-client-secret"
s3_bucket_name       = "globally-unique-production-bucket-name"
frontend_url         = "https://xinyueatelier.com"
backend_image        = "ACCOUNT_ID.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest"
ecs_desired_count    = 1
```

Then run:

```bash
terraform plan
terraform apply
terraform output
```

Wait for the ACM certificate to validate and the ECS service to become healthy. Verify the API through the configured DNS record:

```bash
curl -f https://api.xyatelier.com/actuator/health
aws ecs describe-services --cluster atelier-cluster --services atelier-service --region eu-west-2
```

### 5. Build and publish the frontend

The API URL is compiled into the Vite build:

```bash
cd ../atelier-frontend
npm ci
VITE_API_URL=https://api.xyatelier.com npm run build
aws s3 sync dist/ s3://YOUR_FRONTEND_BUCKET --delete
aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths '/*'
```

Ensure the frontend domain, API domain, and Google OAuth settings agree with `frontend_url` and the backend CORS configuration.

### Stop production resources when not in use

Setting the ECS desired count to zero stops Fargate tasks, but RDS and other resources continue to incur charges:

```bash
aws ecs update-service \
	--cluster atelier-cluster \
	--service atelier-service \
	--desired-count 0 \
	--region eu-west-2
```

To bring the API back:

```bash
aws ecs update-service \
	--cluster atelier-cluster \
	--service atelier-service \
	--desired-count 1 \
	--region eu-west-2
```

## Managing Production Costs

AWS costs add up quickly. Here's a breakdown and strategies to minimize spend while developing:

### Cost Breakdown

| Component | Est. Monthly Cost | Notes |
|---|---|---|
| ECS Fargate (1 task, always running) | $15–30 | Largest variable cost |
| RDS PostgreSQL t3.micro | $10–15 | Database, always running |
| Application Load Balancer (ALB) | $16 | Fixed, always running |
| S3 storage | $0.23/GB | Cheap unless storing many PDFs |
| CloudWatch, NAT, misc | $5–10 | Varies |
| **Total (running)** | **~$50–70** | |
| **Total (paused, RDS on)** | **~$30–35** | ECS at 0, RDS still running |

### Cost-Saving Strategies

**1. Scale ECS Fargate to 0 when not in use (saves ~$20–30/month)**

Stop the backend:
```bash
aws ecs update-service --cluster atelier-cluster --service atelier-service --desired-count 0 --region eu-west-2
```

Resume when needed:
```bash
aws ecs update-service --cluster atelier-cluster --service atelier-service --desired-count 1 --region eu-west-2
```

The ALB and RDS will still incur charges, but this is the biggest single saving.

**2. Stop RDS during extended breaks**

```bash
aws rds stop-db-instance --db-instance-identifier atelier-db --region eu-west-2
```

Resume:
```bash
aws rds start-db-instance --db-instance-identifier atelier-db --region eu-west-2
```

Note: stopped instances incur storage costs but no compute charges.

**3. Develop locally first**

Keep adding folders and patterns to LocalStack (free) until your data model is stable, then migrate to production in one batch. LocalStack is identical to real S3 and PostgreSQL is the same.

**4. Use AWS Free Tier**

If your account is new, you get 12 months of AWS Free Tier:
- 750 hours/month of Fargate (enough for ~1 task running 24/7)
- 750 hours/month of RDS db.t2.micro or db.t3.micro
- 5 GB of S3 storage

This covers your entire setup for free during the trial period.

### Realistic Cost Scenarios

- **Development (local only):** $0
- **Minimal production (ECS paused, RDS on):** ~$35/month
- **Light production (ECS on 8 hrs/day):** ~$60/month
- **Always-on production:** ~$80–100/month

**Recommendation:** Start with local development, deploy to production for final testing, then scale ECS to 0 between sessions.

## Verification Commands

Run the project checks from the repository root:

```bash
make ci
```

The CI target runs backend Checkstyle, Maven verification, JaCoCo reporting, frontend dependency installation, ESLint, Vitest coverage, and the frontend production build.

Individual checks:

```bash
cd atelier-backend && ./mvnw clean verify jacoco:report -Dspring.profiles.active=ci
cd ../atelier-frontend && npm ci && npm run lint && npm test -- --coverage && npm run build
```

## API Summary

### Folders

- `GET /folder` — list root folders
- `GET /folder/{folderId}` — get a folder
- `GET /folder/{parentId}/children` — list child folders
- `POST /folder` and `POST /folder/{parentId}` — create folders
- `PUT /folder/{id}` — update a folder
- `DELETE /folder/{id}` — delete a folder and its descendants/patterns

### Patterns

- `POST /patterns/{folderId}` — upload a PDF
- `GET /patterns/{folderId}/files` — list patterns
- `DELETE /patterns/{patternId}` — delete a pattern
- `GET /patterns/download/{patternId}` — download a pattern
- `GET /patterns/preview/{patternId}` — generate a preview URL

## Security Notes

- Never commit `.env`, `terraform.tfvars`, Terraform state, OAuth client secrets, or AWS access keys.
- Rotate any credential that has ever been stored in `application-secrets.properties`.
- Prefer the ECS task role for S3 access and Secrets Manager for runtime secrets; do not pass long-lived AWS keys to production containers.

Login is via Google OAuth2. On successful login, the backend issues a JWT which the frontend stores and sends as a `Bearer` token on subsequent requests. User records store the Google `sub` claim as `googleId` for account linking.