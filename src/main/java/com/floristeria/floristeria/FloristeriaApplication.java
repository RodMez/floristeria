package com.floristeria.floristeria;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class FloristeriaApplication {

	public static void main(String[] args) {
		// Toda la app trabaja en hora de Colombia (America/Bogota):
		// LocalDateTime.now() debe persistir hora local, no UTC del contenedor.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
		log.info("Zona horaria JVM: {}", java.time.ZoneId.systemDefault());
		SpringApplication.run(FloristeriaApplication.class, args);


	}


}
