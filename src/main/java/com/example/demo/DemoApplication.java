package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {"com.example.demo", "com.app"})
@EntityScan("com.app.models")
@EnableJpaRepositories("com.app.repositories")
@EnableScheduling
@EnableAsync
@RestController
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/")
	public String home() {
		return "CourseFinder Backend is running!";
	}

	@GetMapping("/test")
	public String test() {
		return "Backend test endpoint is working!";
	}
	
	@GetMapping("/api/status")
	public String status() {
		return "CourseFinder Backend API is running and ready!";
	}
} 