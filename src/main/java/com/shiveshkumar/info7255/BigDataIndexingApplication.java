package com.shiveshkumar.info7255;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan("com.shiveshkumar")
public class BigDataIndexingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BigDataIndexingApplication.class, args);
	}
}
