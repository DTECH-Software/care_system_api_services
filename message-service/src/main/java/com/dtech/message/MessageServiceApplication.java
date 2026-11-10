package com.dtech.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class MessageServiceApplication {

	// Build marker: validates the UAT CI/CD pipeline without changing runtime behavior.
	public static void main(String[] args) {
		SpringApplication.run(MessageServiceApplication.class, args);
	}

}
