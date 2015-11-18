package it.reactive.muskel.examples;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;
import static it.reactive.muskel.MuskelProcessor.*;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import static org.junit.Assert.*;

public class RemotePrimeTest {

	public static final int FROM = 100000000;

	public static final int TO = 3000;

	

	 @Test
	public void remoteMultipleMapCount() {

		long numbers = range(FROM, TO)
				.withContext(MuskelContext.builder().client()
						.addAddress("localhost:5701")
						.name("muskel").password("password")
						.build())
				.executeOn(MuskelExecutor.remote())
				.map(i -> range(2, i - 1).takeFirst(k -> i % k == 0)
						.defaultIfEmpty(i).map(k -> k.equals(i) ? k : null)
						.toBlocking().first(), MuskelExecutor.local())
			.filter(i -> i != null)	.doOnNext(
					t -> System.out.println(Thread.currentThread() + " - "
							+ t)).count().toLocal()

				.toBlocking().first();

	//	assertEquals(TOTAL, numbers);
		System.out.println(numbers);
	}

	 
		@Test
		public void test() {

			MuskelContext context = MuskelContext.builder().client()
			.addAddress("131.114.3.250")
			.addAddress("131.114.3.249")
			.addAddress("131.114.2.98").build();
			
			
			MuskelProcessor
					.range(0, 10000)
					.withContext(
							context)
					.executeOn(MuskelExecutor.remote("pianosa"))
					.doOnNext(i -> System.out.println("primo pianosa " + i))
					.executeOn(MuskelExecutor.remote("pianosau"))
					.doOnNext(i -> System.out.println("secondo pianosau " + i))
					.executeOn(MuskelExecutor.remote("phiask"))
					.doOnNext(i -> System.out.println("terzo phiask " + i))

					.toLocal().doOnNext(i -> System.out.println("locale " + i))
					.toBlocking().last();

		}
		
	 
	 @Test
		public void sequentialMap() {
			long start = System.currentTimeMillis();
			try (MuskelProcessor<Long> muskelProcessor = range(FROM, TO)
					.withContext(MuskelContext.builder().local().build())
					.map(i ->  range(2, i / 2)
							// La computazione si ferma al primo numero per cui i %
							// k == 0
							// -> quindi non primo
							.takeFirst(k -> i % k == 0)
							// defaultIfEmpty ritorna i se precedentemente non è
							// arrivato nulla. In questo caso quando il numero è
							// primo
							.defaultIfEmpty(i)
							// Se alla map arriva i vuol dire che era prima e
							// quindi lo restituisce altrimenti ritorna null
							.map(k -> k.equals(i) ? k : null).toBlocking()
							.single())
					// La filter rimuove tutti gli elementi = null
					.filter(i -> i != null).count()) {

				long numbers = muskelProcessor.toBlocking().first();
			//	assertEquals(TOTAL, numbers);
				System.out.println(numbers);
				System.out.println("Total time" + ( System.currentTimeMillis() - start));
			}
		}
	 
	 
	 @Test
		public void parallelMap() {
		 
		 MuskelContext context = MuskelContext.builder().client()
					.addAddress("131.114.3.250")
					.addAddress("131.114.3.249")
					.addAddress("131.114.2.98").build();
		 
		
			long start = System.currentTimeMillis();
			try (MuskelProcessor<Long> muskelProcessor = range(FROM, TO)
					.withContext(context)
					.map(i -> range(2, i / 2)
							// La computazione si ferma al primo numero per cui i %
							// k == 0
							// -> quindi non primo
							.takeFirst(k -> i % k == 0)
							// defaultIfEmpty ritorna i se precedentemente non è
							// arrivato nulla. In questo caso quando il numero è
							// primo
							.defaultIfEmpty(i)
							// Se alla map arriva i vuol dire che era prima e
							// quindi lo restituisce altrimenti ritorna null
							.map(k -> k.equals(i) ? k : null).toBlocking()
							.single(),MuskelExecutor.remote())
					// La filter rimuove tutti gli elementi = null
					.filter(i -> i != null).count()) {

				long numbers = muskelProcessor.toBlocking().first();
			//	assertEquals(TOTAL, numbers);
				System.out.println(numbers);
				System.out.println("Total time" + ( System.currentTimeMillis() - start));
			}
	 }
			
			
	@Test
	public void allRemote() {
		
		try {
			long numbers = executeOn(
					() -> range(FROM, TO).map(
							i -> range(2, i - 1)
									.takeFirst(k -> i % k == 0)
									.defaultIfEmpty(i)
									.map(k -> k.equals(i) ? k : null)
									.toBlocking()
									.first(),
							MuskelExecutor.local()).filter(
							i -> i != null).doOnNext(
									g -> System.out.println("Arrivato "
											+ g)),
					MuskelExecutor.remote())
					.count()
					.doOnNext(g -> System.out.println("client " + g))
					.withContext(
							MuskelContext.builder().client()
									.addAddress("localhost:5701")
									.name("muskel").password("password")
									.build()).toBlocking().first();

			// assertEquals(TOTAL, numbers);
			System.out.println(numbers);
		} catch (Throwable d) {
			d.printStackTrace();
		}
	}
}
