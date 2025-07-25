#!/bin/bash

echo "🔧 Fixing ECS Task Definition - Adding Environment Variables"
echo "============================================================"

REGION="us-east-1"
TASK_DEFINITION="healthapp-task"

echo "1. Downloading current task definition..."
aws ecs describe-task-definition \
  --task-definition $TASK_DEFINITION \
  --region $REGION \
  --query taskDefinition > current-task-definition.json

echo "2. Current task definition saved to current-task-definition.json"

echo ""
echo "3. Please check the current-task-definition.json file and add the following environment variables:"
echo ""
echo "Required Environment Variables:"
echo "  - DB_HOST: Your RDS endpoint"
echo "  - DB_USERNAME: Your RDS username"
echo "  - SPRING_DATASOURCE_PASSWORD: Your RDS password"
echo "  - JWT_SECRET: Your JWT secret key"
echo ""
echo "4. After updating the JSON file, run:"
echo "   aws ecs register-task-definition --cli-input-json file://updated-task-definition.json --region $REGION"
echo ""
echo "5. Then update the service to use the new task definition:"
echo "   aws ecs update-service --cluster healthapp-cluster --service healthapp-service --task-definition healthapp-task --region $REGION"
echo ""
echo "✅ Script completed! Please update the task definition manually." 