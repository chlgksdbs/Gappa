package com.sixheadword.gappa;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@EnableBatchProcessing
@SpringBootApplication
public class GappaApplication {

	public static void main(String[] args) {
		SpringApplication.run(GappaApplication.class, args);
	}

}
