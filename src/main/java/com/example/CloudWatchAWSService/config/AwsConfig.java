package com.example.CloudWatchAWSService.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AwsConfig {

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:ap-south-1}")
    private String region;

    public AWSCredentialsProvider credentialsProvider() {
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            return new AWSStaticCredentialsProvider(credentials);
        }

        return DefaultAWSCredentialsProviderChain.getInstance();
    }

    @Bean
    public AWSLogs awsLogs() {

        return AWSLogsClientBuilder.standard()
                .withCredentials(credentialsProvider())
                .withRegion(region)
                .build();
    }
}
