package dev.pulceo.prm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PulceoServiceManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulceoServiceManagerApplication.class, args);
	}

}
