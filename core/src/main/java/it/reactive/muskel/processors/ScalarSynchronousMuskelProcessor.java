package it.reactive.muskel.processors;

import it.reactive.muskel.internal.publisher.ScalarPublisher;

public class ScalarSynchronousMuskelProcessor<T> extends MuskelProcessorImpl<T> {

    private static final long serialVersionUID = 1L;

    private final T t;

    public static <T> ScalarSynchronousMuskelProcessor<T> create(T value) {
	return new ScalarSynchronousMuskelProcessor<>(value);
    }

    protected ScalarSynchronousMuskelProcessor(T t) {
	super(new ScalarPublisher<>(t));
	this.t = t;
    }

    public T get() {
	return t;
    }

    @Override
    public String toString() {
	return "ScalarSynchronousMuskelProcessor [t=" + t + "]";
    }
}
