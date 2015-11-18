package it.reactive.muskel.internal.executor.impl;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.exceptions.ExecutorNotFoundException;
import it.reactive.muskel.executor.MuskelExecutorService;
import it.reactive.muskel.executor.NamedMuskelExecutorService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@RequiredArgsConstructor
public class MultipleMuskelExecutorService implements
	NamedMuskelExecutorService {

    private final List<NamedMuskelExecutorService> executors = Lists
	    .newArrayList();

    private final MuskelExecutorService defaultExecutorService;

    public MultipleMuskelExecutorService() {
	this(null);
    }

    @Override
    public <T> CompletableFuture<T> submitToKeyOwner(Supplier<T> task,
	    MuskelExecutor key) {
	return getByKey(key).submitToKeyOwner(task, key);
    }

    @Override
    public void execute(Runnable runnable, MuskelExecutor key) {
	getByKey(key).execute(runnable, key);
    }

    @Override
    public boolean supports(MuskelExecutor key) {
	return getByKey(key, false) != null;
    }

    protected MuskelExecutorService getByKey(MuskelExecutor key) {
	return getByKey(key, true);
    }

    protected MuskelExecutorService getByKey(MuskelExecutor key,
	    boolean throwIfNotFound) {
	Optional<NamedMuskelExecutorService> result = executors.stream()
		.filter(executor -> executor.supports(key)).findFirst();
	if (result.isPresent()) {
	    return result.get();
	} else {
	    if (defaultExecutorService == null) {
		if (throwIfNotFound) {
		    throw new ExecutorNotFoundException(
			    "Could not find executor " + key);
		}
	    }
	    return defaultExecutorService;
	}

    }

    public MultipleMuskelExecutorService put(
	    NamedMuskelExecutorService... executors) {
	return addAll(Arrays.asList(executors));
    }

    public MultipleMuskelExecutorService addAll(
	    Iterable<? extends NamedMuskelExecutorService> executors) {
	if (executors != null) {
	    Iterables.addAll(this.executors, executors);
	}
	return this;
    }

    @Override
    public void close() {

	if (defaultExecutorService != null) {
	    try {
		defaultExecutorService.close();
	    } catch (Exception e) {
	    }
	}
	executors.stream().forEach(current -> {
	    try {
		current.close();
	    } catch (Exception e) {
	    }
	});

    }

}
