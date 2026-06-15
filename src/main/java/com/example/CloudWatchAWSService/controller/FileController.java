package com.example.CloudWatchAWSService.controller;

import com.amazonaws.services.logs.model.FilteredLogEvent;
import com.example.CloudWatchAWSService.service.CloudWatchLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final CloudWatchLogService cloudWatchLogService;

    @Value("${aws.cloudwatch.log-group}")
    private String logGroupName;

    @Value("${aws.cloudwatch.log-stream}")
    private String logStreamName;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerLogs(
            @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {

        String message = body.toString();

        // These logs are sent to the configured appenders.
        log.debug("DEBUG - {}", message);
        log.info("INFO - {}", message);
        log.warn("WARN - {}", message);
        log.error("ERROR - {}", message);

        log.info("Log triggered via POST /api/trigger with message='{}'", message);

        String ip = request.getRemoteAddr();

        log.info("Client IP: {}", ip);

        cloudWatchLogService.logMessageToCloudWatch(message);
        return ResponseEntity.ok(Map.of(
                "IP-Address", ip,
                "status", "logs generated",
                "message", message,
                "levels", List.of("DEBUG", "INFO", "WARN", "ERROR"),
                "logGroup", logGroupName,
                "logStream", logStreamName,
                "tip", "Check the configured CloudWatch log group and stream for these events"
        ));
    }


    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogsByMinutes(
            @RequestParam(defaultValue = "30") int minutes) {

        List<FilteredLogEvent> events = cloudWatchLogService.getLogsByMinutes(minutes);

        List<String> messages = events.stream()
                .map(FilteredLogEvent::getMessage)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("logs", messages);

        return ResponseEntity.ok(response);
    }

}