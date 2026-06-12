# CloudWatchService

1. Create S3 bucket:
   aws s3api create-bucket --bucket my-spring-logs-bucket \
   --region ap-south-1 \
   --create-bucket-configuration LocationConstraint=ap-south-1

2. Deploy Lambda:
   mvn clean package
   aws lambda create-function \
   --function-name LogToS3Handler \
   --runtime java17 \
   --handler com.example.lambda.LogToS3Handler::handleRequest \
   --role arn:aws:iam::ACCOUNT:role/lambda-logs-role \
   --zip-file fileb://target/spring-aws-logs-0.0.1-SNAPSHOT.jar \
   --environment Variables={S3_BUCKET_NAME=my-spring-logs-bucket} \
   --timeout 30 --memory-size 256 \
   --region ap-south-1

3. Allow CloudWatch Logs to invoke Lambda:
   (run the aws lambda add-permission command above)

4. Run Spring Boot:
   mvn spring-boot:run

5. Create subscription filter (one time):
   POST http://localhost:8080/api/setup/subscription
   ?lambdaArn=arn:aws:lambda:ap-south-1:ACCOUNT:function:LogToS3Handler

6. Generate some logs:
   POST http://localhost:8080/api/log
   Body: { "message": "hello from Spring Boot" }

7. Check S3:
   aws s3 ls s3://my-spring-logs-bucket/logs/ --recursive