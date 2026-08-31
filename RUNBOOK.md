# XinYueAtelier — Running Local, Dev, and Prod

## Local (laptop, Docker Compose + LocalStack)

**Purpose:** day-to-day coding. Free, fast, uses LocalStack instead of real S3.

```bash
cd XinYueAtelier
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Rebuild just one service after code changes: `docker compose up --build backend`
- Stop (keep data): `docker compose down`
- Stop and WIPE local data: `docker compose down -v` (avoid unless you mean it — this is what caused the earlier "empty database" incident)
- Inspect local Postgres: Beekeeper Studio → `localhost:5432`, db `atelierdb`, user `atelieruser`
- Inspect LocalStack S3: `aws --endpoint-url=http://localhost:4566 s3 ls s3://xin-yue-atelier`

---

## Dev (AWS, ephemeral — spin up only when testing an infra change)

**Purpose:** verify infra changes (RDS, IAM, ECS config) work in real AWS before touching prod. No login testing here — OAuth isn't wired up for dev's changing IP.

**1. Build and push the image (always specify platform on Apple Silicon):**
```bash
docker build --platform linux/amd64 -t atelier-backend:latest ./atelier-backend
docker tag atelier-backend:latest 361769567236.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest
docker push 361769567236.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest
```

**2. Bring dev infra up:**
```bash
cd terraform/environments/dev
terraform init      # only needed the first time
terraform plan       # review before applying
terraform apply
```

**3. If dev is already up and you just pushed a new image, force it to redeploy:**
```bash
aws ecs update-service --cluster atelier-dev-cluster --service atelier-dev-service --force-new-deployment
```

**4. Find the running task's public IP (no ALB in dev):**
```bash
CLUSTER=atelier-dev-cluster
SERVICE=atelier-dev-service
TASK_ARN=$(aws ecs list-tasks --cluster $CLUSTER --service-name $SERVICE --query 'taskArns[0]' --output text)
ENI_ID=$(aws ecs describe-tasks --cluster $CLUSTER --tasks $TASK_ARN --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text)
aws ec2 describe-network-interfaces --network-interface-ids $ENI_ID --query 'NetworkInterfaces[0].Association.PublicIp' --output text
```

**5. Check it's actually healthy:**
```bash
curl http://<the-ip>:8080/actuator/health
```

**6. Debug if it's not:**
```bash
# service-level events (placement failures, image pull errors)
aws ecs describe-services --cluster atelier-dev-cluster --services atelier-dev-service --query 'services[0].events[0:5]'

# find a stopped/crashed task and why
aws ecs list-tasks --cluster atelier-dev-cluster --service-name atelier-dev-service --desired-status STOPPED
aws ecs describe-tasks --cluster atelier-dev-cluster --tasks <task-arn> --query 'tasks[0].{StoppedReason:stoppedReason,Containers:containers[0].{Reason:reason,ExitCode:exitCode}}'

# application logs
# CloudWatch Logs group: /ecs/atelier-dev
```

**7. Tear it down when you're done testing (this is the whole point of dev being ephemeral):**
```bash
terraform destroy
```
Safe — separate state file from prod, `rds_deletion_protection = false` here specifically.

---

## Prod (AWS, always-on, hardened)

**Purpose:** the real, live app. Handle with care — `deletion_protection = true` on RDS blocks accidental deletion, but redeploys and config changes still need to be deliberate.

deploy frontend to prod: 
cd atelier-frontend
docker build --build-arg VITE_API_URL=https://api.xyatelier.com -t atelier-frontend-prod .
docker create --name temp-frontend atelier-frontend-prod
docker cp temp-frontend:/usr/share/nginx/html ./dist-prod
docker rm temp-frontend

aws s3 sync ./dist-prod s3://xinyueatelier-frontend --delete --region eu-west-2


Authenticate Docker to ECR: 

aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin 361769567236.dkr.ecr.eu-west-2.amazonaws.com


**1. Build and push the image (same platform flag matters here too):**
```bash
docker build --platform linux/amd64 -t atelier-backend:latest ./atelier-backend
docker tag atelier-backend:latest 361769567236.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest
docker push 361769567236.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest
```

**2. Roll the new image out** (pushing to `:latest` alone does NOT redeploy — this step is required):
```bash
aws ecs update-service --cluster atelier-cluster --service atelier-service --force-new-deployment
```

**3. Watch the rollout:**
```bash
aws ecs describe-services --cluster atelier-cluster --services atelier-service --query 'services[0].deployments'
```
Wait for `runningCount == desiredCount` and `rolloutState: COMPLETED`.

curl https://api.xyatelier.com/actuator/health

**4. Only when you're changing infrastructure itself (not just app code):**
```bash
cd terraform/environments/prod
terraform plan    # ALWAYS read this fully before apply — this is live infra
terraform apply
```

**5. Access:**
- Backend API: https://api.xyatelier.com
- Frontend:http://xinyueatelier-frontend.s3-website.eu-west-2.amazonaws.com  *(CloudFront + custom domain is the planned upgrade here — see roadmap)*

**6. Checking real prod data:** no direct browser/console view for RDS rows — use your app's own endpoints, or set up ECS Exec / SSM tunneling if you need raw SQL access.

---

## The one habit that prevents most of the pain above

Before pushing any image built on an Apple Silicon Mac: always include `--platform linux/amd64`. This bit both dev and (potentially) prod in this project already. Worth adding to a `Makefile` or build script so it's never a manual thing to remember.
