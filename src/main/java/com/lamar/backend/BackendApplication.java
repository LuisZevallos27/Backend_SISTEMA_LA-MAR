package com.lamar.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Toda la aplicación trabaja en hora de Lima, sin importar la zona del servidor
		TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_ZONA_HORARIA", "America/Lima")));
		SpringApplication.run(BackendApplication.class, args);

	}

}
