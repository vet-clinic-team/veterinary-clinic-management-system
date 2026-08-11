package com.efe.veterinaryclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VeterinaryClinicBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VeterinaryClinicBackendApplication.class, args);
	}

}
