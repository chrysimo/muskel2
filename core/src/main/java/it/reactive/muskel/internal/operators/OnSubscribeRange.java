package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.operator.utils.BackpressureUtils;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AllArgsConstructor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@AllArgsConstructor
public class OnSubscribeRange implements Publisher<Integer>, Serializable {

    private static final long serialVersionUID = 1L;

    private final int start;
    private final int end;

    @Override
    public void subscribe(final Subscriber<? super Integer> child) {
	child.onSubscribe(new OnSubscribeRangeSubscription(child, start, end));
    }

    private static class OnSubscribeRangeSubscription extends AtomicLong
	    implements Subscription, Serializable {

	private static final long serialVersionUID = 1L;

	private final Subscriber<? super Integer> child;

	private final int end;
	private long index;

	private boolean unsubscribed = false;

	public OnSubscribeRangeSubscription(Subscriber<? super Integer> child,
		int start, int end) {
	    this.child = child;
	    this.index = start;
	    this.end = end;
	}

	@Override
	public void request(long n) {
	    if (get() == Long.MAX_VALUE) {
		// already started with fast-path
		return;
	    }
	    if (n == Long.MAX_VALUE && compareAndSet(0L, Long.MAX_VALUE)) {
		// fast-path without backpressure
		fastpath();
	    } else if (n > 0L) {
		long c = BackpressureUtils.getAndAddRequest(this, n);
		if (c == 0L) {
		    // backpressure is requested
		    slowpath(n);
		}
	    }

	}

	void slowpath(long r) {
	    long idx = index;
	    while (true) {
		/*
		 * This complicated logic is done to avoid touching the volatile
		 * `index` and `requested` values during the loop itself. If
		 * they are touched during the loop the performance is impacted
		 * significantly.
		 */
		long fs = end - idx + 1;
		long e = Math.min(fs, r);
		final boolean complete = fs <= r;

		fs = e + idx;
		final Subscriber<? super Integer> o = this.child;

		for (long i = idx; i != fs; i++) {
		    if (isUnsubscribed()) {
			return;
		    }
		    o.onNext((int) i);
		}

		if (complete) {
		    if (isUnsubscribed()) {
			return;
		    }
		    o.onComplete();
		    return;
		}

		idx = fs;
		index = fs;

		r = addAndGet(-e);
		if (r == 0L) {
		    // we're done emitting the number requested so return
		    return;
		}
	    }
	}

	void fastpath() {
	    final long end = this.end + 1L;
	    final Subscriber<? super Integer> o = this.child;
	    for (long i = index; i != end; i++) {
		if (isUnsubscribed()) {
		    return;
		}
		o.onNext((int) i);
	    }
	    if (!isUnsubscribed()) {
		o.onComplete();
	    }
	}

	private boolean isUnsubscribed() {
	    return unsubscribed;
	}

	@Override
	public void cancel() {
	    this.unsubscribed = true;

	}

    }

}
