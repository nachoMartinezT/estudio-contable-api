package com.guidapixel.contable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GuidaContableApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuidaContableApiApplication.class, args);
	}

}
