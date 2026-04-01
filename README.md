# XinYueAtelier

npm run build
aws s3 sync dist/ s3://xinyueatelier-frontend --delete
aws cloudfront create-invalidation --distribution-id E3EMQENX55CGZT --paths "/*"