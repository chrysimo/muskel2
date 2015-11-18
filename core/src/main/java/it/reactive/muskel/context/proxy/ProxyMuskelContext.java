package it.reactive.muskel.context.proxy;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelManagedContext;
import it.reactive.muskel.context.MuskelMessageListener;
import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.executor.MuskelExecutorService;

import java.io.Serializable;

public class ProxyMuskelContext implements MuskelContext, Serializable {

    private static final long serialVersionUID = 1L;

    private MuskelContext target;

    public void setTarget(MuskelContext target) {
	this.target = target;
    }

    protected MuskelContext getRequiredTarget() {
	if (this.target == null) {
	    this.target = ThreadLocalMuskelContext.get();
	}
	if (target == null) {
	    throw new IllegalArgumentException(
		    "Target Muskel Context cannot be null");
	}
	return target;
    }

    @Override
    public String generateIdentifier() {
	return getRequiredTarget().generateIdentifier();
    }

    @Override
    public <T> MuskelQueue<T> getQueue(String name) {

	return getRequiredTarget().getQueue(name);
    }

    @Override
    public void publishMessage(String name, Object value) {
	getRequiredTarget().publishMessage(name, value);

    }

    @Override
    public <T> String addMessageListener(String name,
	    MuskelMessageListener<T> listener) {
	return getRequiredTarget().addMessageListener(name, listener);
    }

    @Override
    public boolean removeMessageListener(String name, String id) {

	return getRequiredTarget().removeMessageListener(name, id);
    }

    @Override
    public MuskelExecutorService getMuskelExecutorService() {
	return getRequiredTarget().getMuskelExecutorService();
    }

    @Override
    public void close() {
	getRequiredTarget().close();

    }

    @Override
    public MuskelManagedContext getManagedContext() {

	return getRequiredTarget().getManagedContext();
    }

    @Override
    public void closeQueue(String name) {
	getRequiredTarget().closeQueue(name);

    }

    @Override
    public void closeMessageListener(String name) {
	getRequiredTarget().closeMessageListener(name);

    }

    @Override
    public String toString() {
	return "ProxyMuskelContext [target=" + target + "]";
    }
}
