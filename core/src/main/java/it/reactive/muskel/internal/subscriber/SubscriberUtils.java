package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.functions.SerializableConsumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SubscriberUtils {

    @RequiredArgsConstructor
    public static class OnErrorSubscriber<T> extends
	    AbstractBasicSubscriber<T, T> {

	private static final long serialVersionUID = 1L;
	@NonNull
	private final SerializableConsumer<Throwable> consumer;

	@Override
	public void onError(Throwable t) {
	    consumer.accept(t);
	}
    }

    @RequiredArgsConstructor
    public static class OnCompleteSubscriber<T> extends
	    AbstractBasicSubscriber<T, T> {

	private static final long serialVersionUID = 1L;
	@NonNull
	private final SerializableConsumer<?> consumer;

	@Override
	public void onComplete() {
	    consumer.accept(null);
	}
    }

    @RequiredArgsConstructor
    public static class OnNextSubscriber<T> extends
	    AbstractBasicSubscriber<T, T> {

	private static final long serialVersionUID = 1L;
	@NonNull
	private final SerializableConsumer<? super T> consumer;

	@Override
	public void onNext(T t) {
	    consumer.accept(t);
	}
    }
}
