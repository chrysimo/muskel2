package it.reactive.muskel;

import it.reactive.muskel.context.MuskelContext;

import java.util.List;

import org.junit.Test;

public class CreateFromSource {

	@Test
	public void localSingle() {

		
		MuskelProcessor.from(1, 2, 3, 4, 5, 6).map(i -> "Hello World " + i)
				.subscribe(s -> System.out.println(s));
	}

	@Test
	public void localSingleToList() {
		MuskelProcessor.from(1, 2, 3, 4, 5, 6).map(i -> "Hello World " + i)
				.toList().subscribe(s -> System.out.println(s));
	}

	@Test
	public void localSingleToListToVariable() {
		List<String> all = MuskelProcessor.from(1, 2, 3, 4, 5, 6)
				.map(i -> "Hello World " + i).toList().toBlocking().first();
		System.out.println(all);
	}

	@Test
	public void localExecuteOnToListToVariable() {
		MuskelProcessor
				.from(1, 2, 3, 4, 5, 6)
				.withContext(MuskelContext.builder().local().build())
				.executeOn(MuskelExecutor.local())
				.map(i -> Thread.currentThread().getName() + " Hello World "
						+ i).toList().subscribe(s -> System.out.println(s));

		System.out.println("Passato");
	}

	@Test
	public void remoteExecuteOnToListToVariable() {
		List<String> all = MuskelProcessor
				.from(1, 2, 3, 4, 5, 6)
				.withContext(
				// Le credenziali devono essere le stesse del server
						MuskelContext.builder().client()
								.addAddress("localhost:5701").name("muskel")
								.password("password").build())
				// Il parametro DEFAULT_REMOTE è uno shortcut che indica di
				// eseguire il comando in uno qualunque dei server remoti
				.executeOn(MuskelExecutor.local())
				.map(i -> Thread.currentThread().getName() + " Hello World "
						+ i)
				// Il comando doOnNext funge da sonda e viene utilizzato in
				// questo caso per far vedere che il programma viene eseguito
				// sul nodo server
				.doOnNext(i -> System.out.println("On Server: " + i)).toList()
				.toLocal().toBlocking().first();

		System.out.println(all);

	}

	@Test
	public void localMapTreadPoolToListToVariable() {
		List<String> all = MuskelProcessor
				.from(1, 2, 3, 4, 5, 6)
				.withContext(MuskelContext.builder().local().build())
				.map(i -> Thread.currentThread().getName() + " Hello World "
						+ i, MuskelExecutor.local())
				.toList()
				.toBlocking()
				.first();
		System.out.println(all);
	}
	
	@Test
	public void remoteMapTreadPoolToListToVariable() {
		List<String> all = MuskelProcessor
				.from(1, 2, 3, 4, 5, 6)
				.withContext(MuskelContext.builder().client()
						.addAddress("localhost:5701").name("muskel")
						.password("password").build())
				.map(i -> Thread.currentThread().getName() + " Hello World "
						+ i, MuskelExecutor.remote())
				.toList()
				.toBlocking()
				.first();
		System.out.println(all);
	}
}
