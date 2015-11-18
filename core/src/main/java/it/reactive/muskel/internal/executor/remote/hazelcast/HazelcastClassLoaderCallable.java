package it.reactive.muskel.internal.executor.remote.hazelcast;

import it.reactive.muskel.context.MuskelInjectAware;
import it.reactive.muskel.internal.executor.remote.hazelcast.utils.ExceptionSerializerUtils;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@MuskelInjectAware
@Slf4j
public class HazelcastClassLoaderCallable<T> extends
	AbstractHazelcastClassLoaderExecutor<T, Supplier<T>> implements
	Callable<T> {

    public HazelcastClassLoaderCallable(String clientUUID, Supplier<T> target) {
	super(clientUUID, target);
    }

    @Override
    protected T doOperation(Supplier<T> target) {
	return target.get();
    }

    @Override
    public T call() throws Exception {
	try {
	    return doOperation();
	} catch (Throwable e) {
	    log.error("Error executing callable " + this, e);

	    throw ExceptionSerializerUtils.serializeException(e);
	}
    }

}
