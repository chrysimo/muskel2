package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.functions.SerializableConsumer;
import it.reactive.muskel.functions.SerializableRunnable;
import it.reactive.muskel.functions.SerializableSubscriber;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

/**
 * Class based of RxJava
 *
 */
public final class LambdaSubscriber<T> extends AtomicReference<Object>
	implements SerializableSubscriber<T>, Subscription {
    /** */
    private static final long serialVersionUID = 1L;
    final SerializableConsumer<? super T> onNext;
    final SerializableConsumer<? super Throwable> onError;
    final SerializableRunnable onComplete;
    final SerializableConsumer<? super Subscription> onSubscribe;

    static final Object CANCELLED = new Object();

    public LambdaSubscriber(SerializableConsumer<? super T> onNext,
	    SerializableConsumer<? super Throwable> onError,
	    SerializableRunnable onComplete,
	    SerializableConsumer<? super Subscription> onSubscribe) {
	super();
	this.onNext = onNext;
	this.onError = onError;
	this.onComplete = onComplete;
	this.onSubscribe = onSubscribe;
    }

    @Override
    public void onSubscribe(Subscription s) {
	if (compareAndSet(null, s)) {
	    onSubscribe.accept(this);
	} else {
	    s.cancel();
	    if (get() != CANCELLED) {
		throw new IllegalStateException("Subscription already set!");
	    }
	}
    }

    @Override
    public void onNext(T t) {
	try {
	    onNext.accept(t);
	} catch (Throwable e) {
	    onError(e);
	}
    }

    @Override
    public void onError(Throwable t) {
	cancel();
	try {
	    onError.accept(t);
	} catch (Throwable e) {
	    e.addSuppressed(t);
	    throw e;
	}
    }

    @Override
    public void onComplete() {
	cancel();

	onComplete.run();

    }

    @Override
    public void request(long n) {
	Object o = get();
	if (o != CANCELLED) {
	    ((Subscription) o).request(n);
	}
    }

    @Override
    public void cancel() {
	Object o = get();
	if (o != CANCELLED) {
	    o = getAndSet(CANCELLED);
	    if (o != CANCELLED && o != null) {
		((Subscription) o).cancel();
	    }
	}
    }
}
