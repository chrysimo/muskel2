package it.reactive.muskel.examples;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyCustomServiceImpl implements MyCustomService{

	@Override
	public String doExecute(String value) {
		log.info("Calling Custom Method with value {}", value);
		
		return "Hello World " + value;
	}

}
