package com.leadstracker.leadstracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling   //responsible for cron jobs
@EnableMethodSecurity
public class LeadsTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeadsTrackerApplication.class, args);
	}

}
