package it.reactive.muskel.internal.operators;

import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.functions.SerializableSubscriberBase;
import it.reactive.muskel.internal.functions.Operator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
public class OperatorDoOnEach<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;

    @NonNull
    private final SerializableSubscriberBase<? super T> doOnEachObserver;

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	return new OperatorDoOnEachSubscriber<T>(doOnEachObserver, t);
    }

    @RequiredArgsConstructor
    private static class OperatorDoOnEachSubscriber<T> implements
	    SerializableSubscriber<T> {

	private static final long serialVersionUID = 1L;
	@NonNull
	private SerializableSubscriberBase<? super T> doOnEachObserver;
	@NonNull
	private final Subscriber<? super T> observer;

	private boolean done = false;

	@Override
	public void onSubscribe(Subscription s) {
	    this.doOnEachObserver = ManagedContextUtils.tryInitialize(s,
		    doOnEachObserver);
	    this.observer.onSubscribe(s);

	}

	@Override
	public void onNext(T t) {
	    if (done) {
		return;
	    }
	    try {
		doOnEachObserver.onNext(t);
	    } catch (Throwable e) {
		onError(e);
		return;
	    }
	    observer.onNext(t);

	}

	@Override
	public void onError(Throwable t) {
	    if (done) {
		return;
	    }
	    done = true;
	    try {
		doOnEachObserver.onError(t);
	    } catch (Throwable e2) {
		observer.onError(e2);
		return;
	    }
	    observer.onError(t);

	}

	@Override
	public void onComplete() {
	    if (done) {
		return;
	    }
	    try {
		doOnEachObserver.onComplete();
	    } catch (Throwable e) {
		onError(e);
		return;
	    }
	    done = true;
	    observer.onComplete();

	}

    }

}
