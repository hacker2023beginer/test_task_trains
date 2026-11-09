package org.vladproj.zhddrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZhddropApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZhddropApplication.class, args);
	}

}
