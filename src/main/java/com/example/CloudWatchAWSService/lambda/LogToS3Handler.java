package com.example.CloudWatchAWSService.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;

/**
 * Triggered by CloudWatch Logs subscription filter.
 * Decodes the compressed log payload and writes each event as a JSON file to S3.
 *
 * Deploy config:
 *   Runtime : java17
 *   Handler : com.example.lambda.LogToS3Handler::handleRequest
 *   Memory  : 256 MB
 *   Timeout : 30 seconds
 *   Env var : S3_BUCKET_NAME = my-spring-logs-bucket
 */
public class LogToS3Handler implements RequestHandler<CloudWatchLogsEvent, String> {

    private static final S3Client s3 = S3Client.create();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String BUCKET = System.getenv("S3_BUCKET_NAME");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    @Override
    public String handleRequest(CloudWatchLogsEvent event, Context context) {

        context.getLogger().log("Received CloudWatch Logs event");

        try {
            // 1. Decode: CloudWatch sends base64-encoded, gzip-compressed JSON
            String encoded = event.getAwsLogs().getData();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            byte[] decompressed = decompress(decoded);
            String json = new String(decompressed, StandardCharsets.UTF_8);

            // 2. Parse the log data envelope
            @SuppressWarnings("unchecked")
            Map<String, Object> logData = mapper.readValue(json, Map.class);

            String logGroup  = (String) logData.get("logGroup");
            String logStream = (String) logData.get("logStream");

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> logEvents =
                    (java.util.List<Map<String, Object>>) logData.get("logEvents");

            context.getLogger().log("Processing " + logEvents.size() +
                    " log events from " + logGroup + "/" + logStream);

            // 3. Write each log event as a separate JSON file in S3
            //    Key pattern: logs/YYYY/MM/DD/<timestamp>-<id>.json
            String datePath = DATE_FMT.format(Instant.now());

            for (Map<String, Object> logEvent : logEvents) {
                String s3Key = buildS3Key(datePath, logEvent, logGroup, logStream);
                String payload = buildPayload(logEvent, logGroup, logStream);
                writeToS3(s3Key, payload, context);
            }

            return "OK — wrote " + logEvents.size() + " log events to S3";

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            throw new RuntimeException("Failed to process log event", e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String buildS3Key(String datePath,
                               Map<String, Object> logEvent,
                               String logGroup,
                               String logStream) {
        // sanitise logGroup for use in S3 key (remove leading slash)
        String group = logGroup.replaceFirst("^/", "").replace("/", "_");
        String id    = String.valueOf(logEvent.get("id"));
        long   ts    = ((Number) logEvent.get("timestamp")).longValue();
        return "logs/" + datePath + "/" + group + "/" + ts + "-" + id + ".json";
    }

    private String buildPayload(Map<String, Object> logEvent,
                                 String logGroup,
                                 String logStream) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp",  Instant.ofEpochMilli(
                ((Number) logEvent.get("timestamp")).longValue()).toString());
        payload.put("id",         logEvent.get("id"));
        payload.put("message",    logEvent.get("message"));
        payload.put("logGroup",   logGroup);
        payload.put("logStream",  logStream);
        payload.put("storedAt",   Instant.now().toString());
        return mapper.writeValueAsString(payload);
    }

    private void writeToS3(String key, String json, Context context) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType("application/json")
                .contentLength((long) bytes.length)
                .build();

        s3.putObject(request, RequestBody.fromBytes(bytes));
        context.getLogger().log("Written: s3://" + BUCKET + "/" + key);
    }

    private byte[] decompress(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(
                new ByteArrayInputStream(compressed))) {
            return gzip.readAllBytes();
        }
    }
}