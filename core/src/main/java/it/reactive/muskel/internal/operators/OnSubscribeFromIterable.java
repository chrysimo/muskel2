package it.reactive.muskel.internal.operators;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class OnSubscribeFromIterable<T> implements Publisher<T>, Serializable {

    private static final long serialVersionUID = 1L;
    final Iterable<? extends T> is;

    public OnSubscribeFromIterable(Iterable<? extends T> iterable) {
	if (iterable == null) {
	    throw new NullPointerException("iterable must not be null");
	}
	this.is = iterable;
    }

    @Override
    public void subscribe(final Subscriber<? super T> o) {
	final Iterator<? extends T> it = is.iterator();
	o.onSubscribe(new IterableProducer<T>(o, it));
    }

    private static final class IterableProducer<T> implements Subscription {
	private final Subscriber<? super T> o;
	private final Iterator<? extends T> it;

	private boolean unsubscribed = false;

	private volatile long requested = 0;
	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<IterableProducer> REQUESTED_UPDATER = AtomicLongFieldUpdater
		.newUpdater(IterableProducer.class, "requested");

	private IterableProducer(Subscriber<? super T> o,
		Iterator<? extends T> it) {
	    this.o = o;
	    this.it = it;
	}

	protected boolean isUnsubscribed() {
	    return unsubscribed;
	}

	@Override
	public void request(long n) {
	    if (REQUESTED_UPDATER.get(this) == Long.MAX_VALUE) {
		// already started with fast-path
		return;
	    }
	    if (n == Long.MAX_VALUE) {
		REQUESTED_UPDATER.set(this, n);
		// fast-path without backpressure
		while (it.hasNext()) {
		    if (isUnsubscribed()) {
			return;
		    }
		    o.onNext(it.next());
		}
		if (!isUnsubscribed()) {
		    o.onComplete();
		}
	    } else if (n > 0) {
		// backpressure is requested
		long _c = getAndAddRequest(REQUESTED_UPDATER, this, n);
		if (_c == 0) {
		    while (true) {
			/*
			 * This complicated logic is done to avoid touching the
			 * volatile `requested` value during the loop itself. If
			 * it is touched during the loop the performance is
			 * impacted significantly.
			 */
			long r = requested;
			long numToEmit = r;
			while (it.hasNext() && --numToEmit >= 0) {
			    if (isUnsubscribed()) {
				return;
			    }
			    o.onNext(it.next());

			}

			if (!it.hasNext()) {
			    if (!isUnsubscribed()) {
				o.onComplete();
			    }
			    return;
			}
			if (REQUESTED_UPDATER.addAndGet(this, -r) == 0) {
			    // we're done emitting the number requested so
			    // return
			    return;
			}

		    }
		}
	    }

	}

	@Override
	public void cancel() {
	    this.unsubscribed = true;

	}
    }

    /**
     * Adds {@code n} to {@code requested} field and returns the value prior to
     * addition once the addition is successful (uses CAS semantics). If
     * overflows then sets {@code requested} field to {@code Long.MAX_VALUE}.
     * 
     * @param requested
     *            atomic field updater for a request count
     * @param object
     *            contains the field updated by the updater
     * @param n
     *            the number of requests to add to the requested count
     * @return requested value just prior to successful addition
     */
    static <T> long getAndAddRequest(AtomicLongFieldUpdater<T> requested,
	    T object, long n) {
	// add n to field but check for overflow
	while (true) {
	    long current = requested.get(object);
	    long next = current + n;
	    // check for overflow
	    if (next < 0)
		next = Long.MAX_VALUE;
	    if (requested.compareAndSet(object, current, next))
		return current;
	}
    }
}
