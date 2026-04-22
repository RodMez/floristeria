package com.floristeria.floristeria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication //(exclude = {DataSourceAutoConfiguration.class})
public class FloristeriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FloristeriaApplication.class, args);

		
	}


}
