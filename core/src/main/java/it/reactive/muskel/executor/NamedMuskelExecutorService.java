package it.reactive.muskel.executor;

import it.reactive.muskel.MuskelExecutor;

public interface NamedMuskelExecutorService extends MuskelExecutorService {

    boolean supports(MuskelExecutor key);

}
