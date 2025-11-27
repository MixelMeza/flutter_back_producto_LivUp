package edu.pe.residencias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResidenciasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResidenciasBackendApplication.class, args);
	}

}
