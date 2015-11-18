package it.reactive.muskel.internal.executor.remote.hazelcast;

import it.reactive.muskel.context.MuskelInjectAware;
import it.reactive.muskel.functions.SerializableRunnable;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@MuskelInjectAware
@Slf4j
public class HazelcastClassLoaderRunnable extends
	AbstractHazelcastClassLoaderExecutor<Void, Runnable> implements
	SerializableRunnable {

    private static final long serialVersionUID = 1L;

    public HazelcastClassLoaderRunnable(String clientUUID, Runnable target) {
	super(clientUUID, target);
    }

    @Override
    protected Void doOperation(Runnable target) {
	try {
	    target.run();
	} catch (Exception e) {
	    log.error("Error executing Runnable " + this, e);
	    final RuntimeException result;
	    if (e instanceof RuntimeException) {
		result = (RuntimeException) e;
	    } else {
		result = new RuntimeException("Error executing Runnable "
			+ this, e);
	    }
	    throw result;
	}
	return null;
    }

    @Override
    public void run() {
	try {
	    doOperation();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}

    }

}
