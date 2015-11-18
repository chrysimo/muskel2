package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.operator.utils.BackpressureUtils;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
public class PublisherStreamSource<T> extends AtomicBoolean implements
	Publisher<T> {

    private static final long serialVersionUID = 1L;

    @NonNull
    private final Stream<? extends T> stream;

    @Override
    public void subscribe(Subscriber<? super T> s) {
	if (compareAndSet(false, true)) {
	    Iterator<? extends T> it;
	    try {
		it = stream.iterator();
	    } catch (Throwable e) {
		s.onError(e);
		return;
	    }
	    s.onSubscribe(new StreamSourceSubscription<>(stream, it, s));
	    return;
	}
	s.onError(new IllegalStateException("Contents already consumed"));

    }

    private static final class StreamSourceSubscription<T> extends AtomicLong
	    implements Subscription {

	private static final long serialVersionUID = 1L;
	private final Iterator<? extends T> it;
	private final Stream<? extends T> stream;
	private final Subscriber<? super T> subscriber;

	private volatile boolean cancelled;

	@SuppressWarnings("unused")
	private volatile int wip;
	@SuppressWarnings("rawtypes")
	static final AtomicIntegerFieldUpdater<StreamSourceSubscription> WIP = AtomicIntegerFieldUpdater
		.newUpdater(StreamSourceSubscription.class, "wip");

	public StreamSourceSubscription(Stream<? extends T> stream,
		Iterator<? extends T> it, Subscriber<? super T> subscriber) {
	    this.stream = stream;
	    this.it = it;
	    this.subscriber = subscriber;
	}

	@Override
	public void request(long n) {
	    if (n <= 0) {
		throw new IllegalArgumentException("n > 0 required but it was "
			+ n);

	    }
	    BackpressureUtils.getAndAddRequest(this, n);
	    drain();
	}

	@Override
	public void cancel() {
	    if (!cancelled) {
		cancelled = true;
		if (WIP.getAndIncrement(this) != 0) {
		    return;
		}
		stream.close();
	    }
	}

	void drain() {
	    if (WIP.getAndIncrement(this) != 0) {
		return;
	    }
	    long r = get();
	    long r0 = r;
	    do {
		if (cancelled) {
		    stream.close();
		    return;
		}
		long e = 0L;

		if (!it.hasNext()) {
		    subscriber.onComplete();
		    return;
		}
		while (r != 0L) {
		    T v = it.next();
		    subscriber.onNext(v);
		    if (cancelled) {
			stream.close();
			return;
		    }
		    if (!it.hasNext()) {
			subscriber.onComplete();
			return;
		    }
		    r--;
		    e--;
		}
		if (e != 0L) {
		    if (r0 != Long.MAX_VALUE) {
			r = addAndGet(e);
		    } else {
			r = Long.MAX_VALUE;
		    }
		}
	    } while (WIP.decrementAndGet(this) != 0);
	}
    }

}
