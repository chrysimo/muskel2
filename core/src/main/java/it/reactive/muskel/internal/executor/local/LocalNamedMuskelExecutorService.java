package it.reactive.muskel.internal.executor.local;

import it.reactive.muskel.MuskelExecutor;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class LocalNamedMuskelExecutorService extends
	AbstractMuskelNamedExcecutorService {

    private static final long serialVersionUID = 1L;
    private final Executor executor;

    public LocalNamedMuskelExecutorService(int nThreads,
	    String... supportedNames) {

	this(Executors
		.newFixedThreadPool(nThreads, new ThreadFactoryBuilder()
			.setNameFormat(Arrays.toString(supportedNames) + "-%d")
			.build()), supportedNames);
	/*
	 * this(new ThreadPoolExecutor( nThreads, nThreads, 0L,
	 * TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1000),
	 * Executors.defaultThreadFactory()), supportedNames);
	 */

    }

    public LocalNamedMuskelExecutorService(Executor executor,
	    String... supportedNames) {
	super(supportedNames);

	this.executor = executor;
    }

    @Override
    public <T> CompletableFuture<T> submitToKeyOwner(Supplier<T> task,
	    MuskelExecutor key) {
	return CompletableFuture.supplyAsync(
		new ContextForwardSupplier<>(task), executor);
    }

    @Override
    public void execute(Runnable runnable, MuskelExecutor key) {
	CompletableFuture.runAsync(new ContextForwardRunnable(runnable),
		executor);

    }

    @Override
    protected boolean isLocal() {
	return true;
    }

    @Override
    public void close() {
	if (this.executor instanceof ExecutorService) {
	    ((ExecutorService) this.executor).shutdown();
	}

    }

    @Override
    public String toString() {
	return "LocalNamedMuskelExecutorService ["
		+ Arrays.toString(supportedNames) + "]";
    }
}
