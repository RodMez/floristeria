package com.floristeria.floristeria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication //(exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
public class FloristeriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FloristeriaApplication.class, args);

		
	}


}
