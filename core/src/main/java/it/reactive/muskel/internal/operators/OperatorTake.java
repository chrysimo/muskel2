package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;

public class OperatorTake<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;

    private final int limit;

    private final boolean unbounded;

    public OperatorTake(int limit) {
	this(limit, false);
    }

    public OperatorTake(int limit, boolean unbounded) {
	if (limit < 0) {
	    throw new IndexOutOfBoundsException("count could not be negative");
	}
	this.limit = limit;
	this.unbounded = unbounded;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	TakeSubscriber<T> result = new TakeSubscriber<>(t, limit, unbounded);

	if (limit == 0) {
	    t.onComplete();
	    result.cancel();
	}
	return result;
    }

    private static class TakeSubscriber<T> extends AbstractSubscriber<T, T> {

	private static final long serialVersionUID = 1L;

	private final int limit;

	private final boolean unbounded;

	private int count;

	private boolean completed;

	private final AtomicLong requested = new AtomicLong(0);

	public TakeSubscriber(Subscriber<? super T> t, int limit,
		boolean unbounded) {
	    super(t);
	    this.limit = limit;
	    this.unbounded = unbounded;
	}

	@Override
	public void request(long n) {
	    if (unbounded) {
		super.request(n);
	    } else {
		if (n > 0 && !completed) {
		    // because requests may happen concurrently use a CAS loop
		    // to
		    // ensure we only request as much as needed, no more no less
		    while (true) {
			long r = requested.get();
			long c = Math.min(n, limit - r);
			if (c == 0)
			    break;
			else if (requested.compareAndSet(r, r + c)) {
			    super.request(c);
			    break;
			}
		    }
		}
	    }
	}

	@Override
	public void onComplete() {
	    if (!completed) {
		completed = true;
		super.onComplete();
	    }
	}

	@Override
	public void onNext(T value) {
	    if (!isUnsubscribed()) {
		boolean stop = ++count >= limit;
		super.onNext(value);
		if (stop && !completed) {
		    completed = true;
		    try {
			super.onComplete();
		    } finally {
			unsubscribe();
		    }
		}
	    }
	}

	@Override
	public void onError(Throwable e) {
	    if (!completed) {
		completed = true;
		try {
		    super.onError(e);
		} finally {
		    unsubscribe();
		}
	    }
	}

	@Override
	public String toString() {
	    return "TakeSubscriber [limit=" + limit + ", unbounded="
		    + unbounded + ", count=" + count + ", completed="
		    + completed + ", requested=" + requested + "]";
	}
    }

}
