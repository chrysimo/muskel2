package it.reactive.muskel.internal.executor.local;

import java.util.function.Supplier;

public class ContextForwardSupplier<T> extends
	AbstractContextForward<T, Supplier<T>> implements Supplier<T> {

    public ContextForwardSupplier(Supplier<T> target) {
	super(target);
    }

    @Override
    protected T doOperation(Supplier<T> target) {
	return target.get();
    }

    @Override
    public T get() {
	return doOperation();
    }

}
