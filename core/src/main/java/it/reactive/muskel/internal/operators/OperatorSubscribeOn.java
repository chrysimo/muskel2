package it.reactive.muskel.internal.operators;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.internal.functions.Operator;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@AllArgsConstructor
public class OperatorSubscribeOn<T> implements Operator<T, MuskelProcessor<T>> {

    private static final long serialVersionUID = 1L;
    private transient final MuskelContext context;

    @NonNull
    private final MuskelExecutor executor;

    @Override
    public SerializableSubscriber<? super MuskelProcessor<T>> apply(
	    final Subscriber<? super T> subscriber) {

	return new SerializableSubscriber<MuskelProcessor<T>>() {

	    private static final long serialVersionUID = 1L;

	    @Override
	    public void onSubscribe(Subscription s) {
		s.request(1);
	    }

	    @Override
	    public void onNext(MuskelProcessor<T> p) {
		context.getMuskelExecutorService().execute(() -> {

		    final Thread t = Thread.currentThread();
		    p.subscribe(new SerializableSubscriber<T>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onSubscribe(Subscription s) {
			    if (Thread.currentThread() == t) {
				subscriber.onSubscribe(s);
			    } else {
				context.getMuskelExecutorService().execute(
					() -> {
					    subscriber.onSubscribe(s);
					}, executor);
			    }

			}

			@Override
			public void onNext(T t) {
			    subscriber.onNext(t);

			}

			@Override
			public void onError(Throwable t) {
			    subscriber.onError(t);

			}

			@Override
			public void onComplete() {
			    subscriber.onComplete();

			}
		    });

		}, executor);

	    }

	    @Override
	    public void onError(Throwable t) {
		subscriber.onError(t);

	    }

	    @Override
	    public void onComplete() {
		// ignore because this is a nested MuskelProcessor and we expect
		// only 1 MuskelProcessor<T> emitted to onNext

	    }
	};

    }

}
