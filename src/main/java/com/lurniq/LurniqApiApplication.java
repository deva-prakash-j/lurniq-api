package com.lurniq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {SystemMetricsAutoConfiguration.class})
public class LurniqApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LurniqApiApplication.class, args);
	}

}
