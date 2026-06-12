package com.example.CloudWatchAWSService;

import com.example.CloudWatchAWSService.service.CloudWatchLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class CloudWatchAwsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudWatchAwsServiceApplication.class, args);
	}

	@Bean
    ApplicationRunner init(CloudWatchLogService logService) {
		return args -> {
			logService.ensureLogGroupExists();
			log.info("Spring Boot started — logs streaming to CloudWatch");
		};
	}

}
