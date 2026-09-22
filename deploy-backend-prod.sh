#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/atelier-backend"

docker build --platform linux/amd64 -t atelier-backend:latest .
docker tag atelier-backend:latest 361769567236.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest
docker push 361769567236.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest

aws ecs update-service --cluster atelier-cluster --service atelier-service --force-new-deployment

echo "Waiting for rollout..."
aws ecs wait services-stable --cluster atelier-cluster --services atelier-service
echo "Rollout complete. Checking health..."
curl -sf https://api.xyatelier.com/actuator/health && echo " — healthy"