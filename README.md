# CloudWatchService

Spring Boot service that writes test log messages to AWS CloudWatch Logs.

## Configuration

Set AWS credentials through environment variables or another supported AWS credentials provider:

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

The default application settings are in `src/main/resources/application.yaml`:

```yaml
server:
  port: 8081

aws:
  region: ap-south-1
  cloudwatch:
    log-group: sprint-boot-application
    log-stream: app-log-stream
```

## IAM Permissions

Use `src/main/resources/IAM.json` as the minimum policy shape for the credentials used by the app.

## Run

```bash
mvn spring-boot:run
```

## Generate Logs

```bash
curl -X POST http://localhost:8081/api/trigger \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello from Spring Boot"}'
```

Then check the configured CloudWatch log group and stream.
