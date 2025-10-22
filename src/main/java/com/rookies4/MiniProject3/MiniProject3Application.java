package com.rookies4.MiniProject3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiniProject3Application {

	public static void main(String[] args) {
		SpringApplication.run(MiniProject3Application.class, args);
	}

}
