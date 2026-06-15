package com.example.CloudWatchAWSService.service;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudWatchLogService {

    private final AWSLogs cloudWatchLogs;


    @Value("${aws.cloudwatch.log-group}")
    private String logGroupName;

    @Value("${aws.cloudwatch.log-stream}")
    private String logStreamName;

    /**
     * Send log message to AWS CloudWatch
     */
    public synchronized void logMessageToCloudWatch(String message) {

        try {
            ensureLogGroupAndStreamExist();

            // Create Log Event
            InputLogEvent logEvent = new InputLogEvent()
                    .withTimestamp(System.currentTimeMillis())
                    .withMessage(message);

            // Get sequence token
            String sequenceToken = getSequenceToken();

            // Build request
            PutLogEventsRequest request = new PutLogEventsRequest()
                    .withLogGroupName(logGroupName)
                    .withLogStreamName(logStreamName)
                    .withLogEvents(Collections.singletonList(logEvent));

            // Add sequence token if present
            if (sequenceToken != null) {
                request.setSequenceToken(sequenceToken);
            }

            // Send log
            PutLogEventsResult result = cloudWatchLogs.putLogEvents(request);

            log.info("Successfully sent log to CloudWatch.");
            log.debug("Next Sequence Token : {}", result.getNextSequenceToken());

        } catch (ResourceNotFoundException ex) {

            log.error("CloudWatch Log Group or Log Stream does not exist.", ex);

        } catch (InvalidSequenceTokenException ex) {

            log.error("Invalid Sequence Token.", ex);

        } catch (DataAlreadyAcceptedException ex) {

            log.warn("Duplicate log event. Already accepted.", ex);

        } catch (Exception ex) {

            log.error("Error sending log to CloudWatch.", ex);

        }
    }

    /**
     * Fetch latest upload sequence token
     */
    private String getSequenceToken() {

        DescribeLogStreamsRequest request = new DescribeLogStreamsRequest()
                .withLogGroupName(logGroupName)
                .withLogStreamNamePrefix(logStreamName);

        DescribeLogStreamsResult result =
                cloudWatchLogs.describeLogStreams(request);

        for (LogStream stream : result.getLogStreams()) {

            if (stream.getLogStreamName().equals(logStreamName)) {

                return stream.getUploadSequenceToken();
            }
        }

        return null;
    }

    private void ensureLogGroupAndStreamExist() {
        try {
            cloudWatchLogs.createLogGroup(new CreateLogGroupRequest(logGroupName));
            log.info("Created CloudWatch log group: {}", logGroupName);
        } catch (ResourceAlreadyExistsException ex) {
            log.debug("CloudWatch log group already exists: {}", logGroupName);
        }

        try {
            cloudWatchLogs.createLogStream(new CreateLogStreamRequest(logGroupName, logStreamName));
            log.info("Created CloudWatch log stream: {}", logStreamName);
        } catch (ResourceAlreadyExistsException ex) {
            log.debug("CloudWatch log stream already exists: {}", logStreamName);
        }
    }

    public List<FilteredLogEvent> getLogsByMinutes(int minutes) {

        long endTime = Instant.now().toEpochMilli();
        long startTime = Instant.now()
                .minusSeconds(minutes * 60L)
                .toEpochMilli();

        FilterLogEventsRequest request = new FilterLogEventsRequest()
                .withLogGroupName(logGroupName)
                .withStartTime(startTime)
                .withEndTime(endTime);

        FilterLogEventsResult result = cloudWatchLogs.filterLogEvents(request);

        return result.getEvents();
    }
}
