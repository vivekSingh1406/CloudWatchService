package com.example.CloudWatchAWSService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudWatchLogService {

    private final CloudWatchLogsClient logsClient;

    @Value("${aws.cloudwatch.log-group}")
    private String logGroup;

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch all log events from the last N minutes using filterLogEvents.
    // filterLogEvents lets you search across ALL log streams in a log group
    // and supports time-range filtering — perfect for "last 30 minutes".
    // ─────────────────────────────────────────────────────────────────────────
    public List<FilteredLogEvent> getLogsByMinutes(int minutes) {

        // Calculate the time window
        Instant now       = Instant.now();
        Instant startTime = now.minusSeconds((long) minutes * 60);

        log.info("Querying CloudWatch logs from {} to {} ({} min)",
                startTime, now, minutes);

        List<FilteredLogEvent> allEvents = new ArrayList<>();
        String nextToken = null;

        // CloudWatch paginates results — loop until all pages are fetched
        do {
            FilterLogEventsRequest.Builder requestBuilder = FilterLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .startTime(startTime.toEpochMilli())   // milliseconds since epoch
                    .endTime(now.toEpochMilli())            // milliseconds since epoch
                    .limit(10_000);                         // max per page

            if (nextToken != null) {
                requestBuilder.nextToken(nextToken);
            }

            FilterLogEventsResponse response =
                    logsClient.filterLogEvents(requestBuilder.build());

            allEvents.addAll(response.events());
            nextToken = response.nextToken();   // null when last page reached

            log.debug("Fetched page with {} events, nextToken={}",
                    response.events().size(), nextToken);

        } while (nextToken != null);

        log.info("Total log events fetched: {} for last {} minutes", allEvents.size(), minutes);
        return allEvents;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Creates the CloudWatch Log Group if it doesn't exist yet.
    // Call once at startup via ApplicationRunner.
    // ─────────────────────────────────────────────────────────────────────────
    public void ensureLogGroupExists() {
        try {
            logsClient.createLogGroup(r -> r.logGroupName(logGroup));
            log.info("Created CloudWatch log group: {}", logGroup);
        } catch (ResourceAlreadyExistsException e) {
            log.debug("Log group already exists: {}", logGroup);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Creates a Subscription Filter so CloudWatch automatically triggers
    // your Lambda for every log event written to this log group.
    // Run once via setup API or CLI.
    // ─────────────────────────────────────────────────────────────────────────
    public void createSubscriptionFilter(String lambdaArn) {
        PutSubscriptionFilterRequest request = PutSubscriptionFilterRequest.builder()
                .logGroupName(logGroup)
                .filterName("AllLogsToS3")
                .filterPattern("")                              // "" = match all events
                .destinationArn(lambdaArn)
                .distribution(Distribution.BY_LOG_STREAM)
                .build();

        logsClient.putSubscriptionFilter(request);
        log.info("Subscription filter created — Lambda: {}", lambdaArn);
    }
}