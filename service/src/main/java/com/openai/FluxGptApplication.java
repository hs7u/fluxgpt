package com.openai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableR2dbcAuditing
@SpringBootApplication
public class FluxGptApplication {

	public static void main(String[] args) {
		SpringApplication.run(FluxGptApplication.class, args);
	}

}
