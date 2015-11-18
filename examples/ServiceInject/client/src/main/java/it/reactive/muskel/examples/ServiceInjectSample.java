package it.reactive.muskel.examples;

import java.util.List;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelInjectAware;
import it.reactive.muskel.examples.MyCustomService;
import it.reactive.muskel.functions.SerializableFunction;

import javax.inject.Inject;

import org.junit.Test;

public class ServiceInjectSample {

	@Test
	public void doRemoteTest() {
		
		
		List<String> results =  MuskelProcessor
				.from(1, 2, 3, 4, 5, 6)
				.withContext(
						MuskelContext.builder().client()
								.addAddress("localhost:5701").name("muskel")
								.password("password").build())
				.map(new MyInjectFuntion(), MuskelExecutor.remote())
				.toList().toBlocking().first();
		System.out.println(results);
	}

	@MuskelInjectAware
	public static class MyInjectFuntion implements
			SerializableFunction<Integer, String> {

		private static final long serialVersionUID = 1L;

		@Inject
		private MyCustomService myService;

		@Override
		public String apply(Integer t) {
			return myService.doExecute(String.valueOf(t));
		}

	}

}
