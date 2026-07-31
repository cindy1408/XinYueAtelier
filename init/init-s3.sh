#!/bin/bash
awslocal s3 mb s3://${S3_BUCKET}
awslocal s3api put-bucket-cors --bucket ${S3_BUCKET} --cors-configuration file:///etc/localstack/init/ready.d/cors-config.json