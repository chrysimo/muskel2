package it.reactive.muskel.server.examples.config;

import it.reactive.muskel.examples.MyCustomService;
import it.reactive.muskel.examples.MyCustomServiceImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MyCustomServiceConfig {

	@Bean
	public MyCustomService createMyCustomService() {
		log.info("Starting MyCustomService!!!");
		return new MyCustomServiceImpl();
	}
}
