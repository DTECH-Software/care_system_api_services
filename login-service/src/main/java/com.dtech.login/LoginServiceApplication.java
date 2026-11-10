package com.dtech.login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableJpaAuditing  //(auditorAwareRef = "auditorAwareService")
public class LoginServiceApplication {

	// Build marker: validates the UAT CI/CD pipeline without changing runtime behavior.
	public static void main(String[] args) {
		SpringApplication.run(LoginServiceApplication.class, args);
	}

}
