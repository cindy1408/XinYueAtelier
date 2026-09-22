#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/atelier-frontend"

rm -rf ./dist-prod
docker build --build-arg VITE_API_URL=https://api.xyatelier.com -t atelier-frontend-prod .
docker create --name temp-frontend atelier-frontend-prod
docker cp temp-frontend:/usr/share/nginx/html ./dist-prod
docker rm temp-frontend

aws s3 sync ./dist-prod s3://xinyueatelier-frontend --delete --region eu-west-2
aws cloudfront create-invalidation --distribution-id E3EMQENX55CGZT --paths "/*"

echo "Deployed. Verifying asset hash match..."
sleep 5
curl -s https://xyatelier.com/ | grep -oE 'assets/index-[A-Za-z0-9]+\.(js|css)'
aws s3 ls s3://xinyueatelier-frontend/assets/