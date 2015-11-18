package it.reactive.muskel.processors;

public class ThrowMuskelProcessor<T> extends MuskelProcessorImpl<T> {

    private static final long serialVersionUID = 1L;

    public ThrowMuskelProcessor(final Throwable exception) {
	super(subscriber -> subscriber.onError(exception));
    }

}
