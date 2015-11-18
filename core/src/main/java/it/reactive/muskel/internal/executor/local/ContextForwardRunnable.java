package it.reactive.muskel.internal.executor.local;

import it.reactive.muskel.context.MuskelContext;

public class ContextForwardRunnable extends
	AbstractContextForward<Void, Runnable> implements Runnable {

    public ContextForwardRunnable(Runnable target) {
	super(target);
    }

    public ContextForwardRunnable(Runnable target, MuskelContext context) {
	super(target, context);
    }

    @Override
    public void run() {
	doOperation();

    }

    @Override
    protected Void doOperation(Runnable target) {
	target.run();
	return null;
    }

}
