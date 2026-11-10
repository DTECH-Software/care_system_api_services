package com.dtech.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class RegistryServiceApplication {

	// Build marker: validates the UAT CI/CD pipeline without changing runtime behavior.
	public static void main(String[] args) {
		SpringApplication.run(RegistryServiceApplication.class, args);
	}

}
