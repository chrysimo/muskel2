package it.reactive.muskel.examples;

import static it.reactive.muskel.MuskelProcessor.empty;
import static it.reactive.muskel.MuskelProcessor.just;
import static it.reactive.muskel.MuskelProcessor.range;
import static org.junit.Assert.assertEquals;
import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;

import org.junit.Test;

/**
 * La classe effettua il calcolo dei numeri primi appartenenti ad un certo range
 * verificando, per ognuno di essi, se divisibile per i primi n/2
 *
 * Gli esempi mostrano diverse modalità con cui effettuare il calcolo
 */
public class LocalPrimeTest {

	// public static final int FROM = 1500000;

	// public static final int TO = 1600000;

	public static final int FROM = 2;

	// public static final int TO = 10000-1;

	public static final int COUNT = 100000 - 1;

	 public static final long TOTAL = 9592;

	// public static final long TOTAL = 1229;

	@Test
	public void sequentialFlatMapOnly() {

		try (MuskelProcessor<Long> muskelProcessor = range(FROM, COUNT)
				.withContext(MuskelContext.builder().local().build())
				.flatMap(i -> range(2, i / 2)
				// La computazione si ferma al primo numero per cui i % k == 0
				// -> quindi non primo
						.takeFirst(k -> i % k == 0)
						// defaultIfEmpty ritorna i se precedentemente non è
						// arrivato nulla. In questo caso quando il numero è
						// primo
						.defaultIfEmpty(i)
						// Se alla flatMap arriva i vuol dire che era prima e
						// quindi lo elette usando just() altrimenti genera uno
						// stream vuoto
						.flatMap(x -> x.equals(i) ? just(i) : empty()))
				// La count conta il numero di elementi presenti
				.count()) {

			long numbers = muskelProcessor.toBlocking().first();
			assertEquals(TOTAL, numbers);
			System.out.println(numbers);
		}

	}

	@Test
	public void parallelFlatMapOnly() {

		try (MuskelProcessor<Long> muskelProcessor = range(FROM, COUNT)
				.withContext(
						MuskelContext.builder().local().defaultPoolSize(16)
								.build())
				.flatMap(
						i -> range(2, i / 2).takeFirst(k -> i % k == 0)
								.defaultIfEmpty(i)
								.flatMap(x -> x.equals(i) ? just(i) : empty()),
						MuskelExecutor.local()).count()) {

			long numbers = muskelProcessor.toBlocking().first();
			assertEquals(TOTAL, numbers);
			System.out.println(numbers);
		}

	}

	@Test
	public void sequentialFlatMapAndMap() {

		try (MuskelProcessor<Long> muskelProcessor = range(FROM, COUNT)
				.withContext(MuskelContext.builder().local().build())
				.flatMap(
						i -> range(2, i / 2).takeFirst(k -> i % k == 0)
								.defaultIfEmpty(i)
								.map(x -> x.equals(i) ? x : null))
				.filter(i -> i != null).count()) {

			long numbers = muskelProcessor.toBlocking().first();
			assertEquals(TOTAL, numbers);
			System.out.println(numbers);

		}
	}

	@Test
	public void parallelFlatMapAndMap() {

		try (MuskelProcessor<Long> muskelProcessor = range(FROM, COUNT)
				.withContext(MuskelContext.builder().local().build())
				.flatMap(
						i -> range(2, i / 2).takeFirst(k -> i % k == 0)
								.defaultIfEmpty(i)
								.map(x -> x.equals(i) ? x : null),
						MuskelExecutor.local()).filter(i -> i != null)
				.count()) {

			long numbers = muskelProcessor.toBlocking().first();
			assertEquals(TOTAL, numbers);
			System.out.println(numbers);

		}
	}

	@Test
	public void sequentialMap() {

		try (MuskelProcessor<Long> muskelProcessor = range(FROM, COUNT)
				.withContext(MuskelContext.builder().local().build())
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
						.first())
				// La filter rimuove tutti gli elementi = null
				.filter(i -> i != null).count()) {

			long numbers = muskelProcessor.toBlocking().first();
			assertEquals(TOTAL, numbers);
			System.out.println(numbers);
		}
	}

	@Test
	public void parallelMap() {
	
		long start = System.currentTimeMillis();
		try (MuskelProcessor<Long> muskelProcessor = range(FROM, COUNT)
				.withContext(MuskelContext.builder().local().build())
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
						.single(),MuskelExecutor.local())
				// La filter rimuove tutti gli elementi = null
				.filter(i -> i != null).count()) {

			long numbers = muskelProcessor.toBlocking().first();
			assertEquals(TOTAL, numbers);
			System.out.println(numbers);
			System.out.println( System.currentTimeMillis() - start);
		}

	}	
	
	
	
	@Test
	public void parallelMap2() {
	
	
		try (MuskelProcessor<Integer> muskelProcessor = range(FROM, COUNT)
				.withContext(MuskelContext.builder().local().build())
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
						.first(),MuskelExecutor.local())
				// La filter rimuove tutti gli elementi = null
				.filter(i -> i != null)) {

			 muskelProcessor.toBlocking().forEach(current -> System.out.println(current));
		
		}

	}	
}
