package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

public class BlockingSubscriber<T> extends AtomicReference<Subscription>
	implements SerializableSubscriber<T>, Subscription {
    /** */
    private static final long serialVersionUID = -4875965440900746268L;

    static final Subscription CANCELLED = new Subscription() {
	@Override
	public void request(long n) {

	}

	@Override
	public void cancel() {

	}
    };

    public static final Object TERMINATED = new Object();

    final Queue<Object> queue;

    public BlockingSubscriber(Queue<Object> queue) {
	this.queue = queue;
    }

    @Override
    public void onSubscribe(Subscription s) {
	if (!compareAndSet(null, s)) {
	    s.cancel();
	    if (get() != CANCELLED) {
		onError(new IllegalStateException("Subscription already set"));
	    }
	    return;
	}
	queue.offer(SentinelUtils.subscription(this));
    }

    @Override
    public void onNext(T t) {
	queue.offer(SentinelUtils.next(t));
    }

    @Override
    public void onError(Throwable t) {
	queue.offer(SentinelUtils.error(t));
    }

    @Override
    public void onComplete() {
	queue.offer(SentinelUtils.complete());
    }

    @Override
    public void request(long n) {
	get().request(n);
    }

    @Override
    public void cancel() {
	Subscription s = get();
	if (s != CANCELLED) {
	    s = getAndSet(CANCELLED);
	    if (s != CANCELLED && s != null) {
		s.cancel();
		queue.offer(TERMINATED);
	    }
	}
    }

    public boolean isCancelled() {
	return get() == CANCELLED;
    }
}
