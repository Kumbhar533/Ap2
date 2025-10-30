package com.veefin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Ap2PocApplication {

	public static void main(String[] args) {
		SpringApplication.run(Ap2PocApplication.class, args);
	}

}
