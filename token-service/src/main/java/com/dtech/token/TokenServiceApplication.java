package com.dtech.token;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing //(auditorAwareRef = "auditorAwareService")
public class TokenServiceApplication {

	// Build marker: validates the UAT CI/CD pipeline without changing runtime behavior.
	public static void main(String[] args) {
		SpringApplication.run(TokenServiceApplication.class, args);
	}

}
