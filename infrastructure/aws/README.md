# AWS deployment lab

This lab deploys the container to AWS App Runner from Amazon ECR. It intentionally uses
CloudFormation so infrastructure changes can follow the same Git review process as code.

Prerequisites: AWS CLI credentials, Docker, and an ECR repository.

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=us-east-1
REPOSITORY=automation-mission-control

aws ecr create-repository --repository-name "$REPOSITORY"
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
docker build -t "$REPOSITORY" .
docker tag "$REPOSITORY:latest" "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:latest"
docker push "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:latest"

aws cloudformation deploy --stack-name automation-mission-control \
  --template-file infrastructure/aws/template.yaml --capabilities CAPABILITY_IAM \
  --parameter-overrides ImageRepository="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:latest"
```

Production exercise: replace the in-memory H2 database with Amazon RDS, store credentials
in Secrets Manager, add a VPC connector, alarms, a custom domain, and a rollback runbook.
