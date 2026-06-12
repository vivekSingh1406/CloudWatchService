package com.example.CloudWatchAWSService.controller;

import com.example.CloudWatchAWSService.service.CloudWatchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final CloudWatchLogService logService;

    // ─────────────────────────────────────────────────────────────────────────
    // API 1 — GET /api/logs?minutes=30
    //
    // Returns all CloudWatch log events from the last N minutes.
    // Pass ?minutes=20 or ?minutes=50 etc.
    //
    // Example:
    //   GET http://localhost:8080/api/logs?minutes=30
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogsByMinutes(
            @RequestParam(defaultValue = "30") int minutes) {

        log.info("Fetching logs for last {} minutes", minutes);

        List<FilteredLogEvent> events = logService.getLogsByMinutes(minutes);

        return ResponseEntity.ok(Map.of(
                "minutes",    minutes,
                "totalCount", events.size(),
                "logGroup",   "/spring-demo/app-logs",
                "from",       java.time.Instant.now().minusSeconds(minutes * 60L).toString(),
                "to",         java.time.Instant.now().toString(),
                "logs",       events.stream().map(e -> Map.of(
                        "timestamp", java.time.Instant.ofEpochMilli(e.timestamp()).toString(),
                        "message",   e.message(),
                        "logStream", e.logStreamName()
                )).toList()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API 2 — POST /api/trigger
    //
    // Hit any POST endpoint to generate logs at all levels (DEBUG/INFO/WARN/ERROR).
    // These logs automatically stream to CloudWatch via Logback appender.
    // Then use API 1 to fetch them back.
    //
    // Request body (JSON):
    //   { "message": "your custom message" }
    //
    // Example:
    //   POST http://localhost:8080/api/trigger
    //   Body: { "message": "payment failed for user 123" }
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerLogs(
            @RequestBody(required = false) Map<String, String> body) {

        String message = (body != null)
                ? body.getOrDefault("message", "default test log")
                : "default test log";

        // These 4 lines generate logs at every level → stream to CloudWatch
        log.debug("DEBUG — {}", message);
        log.info ("INFO  — {}", message);
        log.warn ("WARN  — {}", message);
        log.error("ERROR — {}", message);

        log.info("Log triggered via POST /api/trigger with message='{}'", message);

        return ResponseEntity.ok(Map.of(
                "status",    "logs generated",
                "message",   message,
                "levels",    List.of("DEBUG", "INFO", "WARN", "ERROR"),
                "logGroup",  "/spring-demo/app-logs",
                "logStream", "spring-boot-stream",
                "tip",       "Now call GET /api/logs?minutes=1 to see these logs"
        ));
    }
}