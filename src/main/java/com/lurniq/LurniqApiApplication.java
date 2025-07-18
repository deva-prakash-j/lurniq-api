package com.lurniq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration;

@SpringBootApplication(exclude = {
    SystemMetricsAutoConfiguration.class,
    MetricsAutoConfiguration.class,
    TomcatMetricsAutoConfiguration.class
})
public class LurniqApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LurniqApiApplication.class, args);
	}

}
