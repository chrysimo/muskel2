package it.reactive.muskel.executor;

import it.reactive.muskel.MuskelExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * This is the abstraction of the Executor Service
 *
 */
public interface MuskelExecutorService extends AutoCloseable {

    /**
     * Submits a task to the owner of the specified key and returns a Future
     * representing that task.
     * 
     * @param <T>
     *            the key type
     * @param task
     *            task submitted to the owner of the specified key
     * @param executor
     *            the specified executor
     * @return a {@link CompletableFuture} representing pending completion of
     *         the task
     */
    <T> CompletableFuture<T> submitToKeyOwner(Supplier<T> task,
	    MuskelExecutor executor);

    /**
     * Submits a runnable to the owner of the specified key
     *
     * @param runnable
     *            the runnable
     * @param executor
     *            the specified executor
     */
    void execute(Runnable runnable, MuskelExecutor executor);

}
