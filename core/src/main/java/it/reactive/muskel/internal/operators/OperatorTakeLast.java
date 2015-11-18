package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.operator.utils.BackpressureUtils;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class OperatorTakeLast<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;

    private final int count;

    public OperatorTakeLast(int count) {
	if (count < 0) {
	    throw new IndexOutOfBoundsException("count could not be negative");
	}
	this.count = count;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	return new TakeLastSubscriber<>(t, count);
    }

    private static class TakeLastSubscriber<T> extends AbstractSubscriber<T, T> {

	private static final Object ON_COMPLETED_SENTINEL = new Serializable() {
	    private static final long serialVersionUID = 1;

	    @Override
	    public String toString() {
		return "Notification=>Completed";
	    }
	};

	private static final Object ON_NEXT_NULL_SENTINEL = new Serializable() {
	    private static final long serialVersionUID = 2;

	    @Override
	    public String toString() {
		return "Notification=>NULL";
	    }
	};

	private final int count;

	private final Deque<Object> deque = new ArrayDeque<Object>();

	private static final long serialVersionUID = 1L;

	private volatile boolean emittingStarted = false;

	private volatile long requested = 0;
	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<TakeLastSubscriber> REQUESTED_UPDATER = AtomicLongFieldUpdater
		.newUpdater(TakeLastSubscriber.class, "requested");

	public TakeLastSubscriber(Subscriber<? super T> t, int count) {
	    super(t);
	    this.count = count;
	}

	@Override
	public void onSubscribe(Subscription s) {
	    super.onSubscribe(s);
	    super.request(Long.MAX_VALUE);

	};

	@Override
	public void request(long n) {
	    BackpressureUtils.getAndAddRequest(REQUESTED_UPDATER, this, n);

	    if (n > 0) {
		if (requested == Long.MAX_VALUE) {
		    return;
		}
		long _c = BackpressureUtils.getAndAddRequest(REQUESTED_UPDATER,
			this, n);
		if (!emittingStarted) {
		    // we haven't started yet, so record what was requested and
		    // return
		    return;
		}
		emit(_c);
	    }
	}

	@Override
	public void onComplete() {
	    deque.offer(ON_COMPLETED_SENTINEL);
	    if (!emittingStarted) {
		emittingStarted = true;
		emit(0);
	    }
	}

	@Override
	public void onNext(T value) {
	    if (count == 0) {
		// If count == 0, we do not need to put value into deque and
		// remove it at once. We can ignore the value directly.
		return;
	    }
	    if (deque.size() == count) {
		deque.removeFirst();
	    }
	    deque.offerLast(value == null ? ON_NEXT_NULL_SENTINEL : value);
	}

	@Override
	public void onError(Throwable e) {
	    deque.clear();
	    super.onError(e);
	}

	void emit(long previousRequested) {
	    if (requested == Long.MAX_VALUE) {
		// fast-path without backpressure
		if (previousRequested == 0) {
		    try {
			for (Object value : deque) {
			    if (isUnsubscribed())
				return;
			    doNext(value);
			}
		    } catch (Throwable e) {
			super.onError(e);
		    } finally {
			deque.clear();
		    }
		} else {
		    // backpressure path will handle Long.MAX_VALUE and emit the
		    // rest events.
		}
	    } else {
		// backpressure is requested
		if (previousRequested == 0) {
		    while (true) {
			/*
			 * This complicated logic is done to avoid touching the
			 * volatile `requested` value during the loop itself. If
			 * it is touched during the loop the performance is
			 * impacted significantly.
			 */
			long numToEmit = requested;
			int emitted = 0;
			Object o;
			while (--numToEmit >= 0 && (o = deque.poll()) != null) {
			    if (isUnsubscribed()) {
				return;
			    }
			    if (doNext(o)) {
				// terminal event
				return;
			    } else {
				emitted++;
			    }
			}
			for (;;) {
			    long oldRequested = requested;
			    long newRequested = oldRequested - emitted;
			    if (oldRequested == Long.MAX_VALUE) {
				// became unbounded during the loop
				// continue the outer loop to emit the rest
				// events.
				break;
			    }
			    if (REQUESTED_UPDATER.compareAndSet(this,
				    oldRequested, newRequested)) {
				if (newRequested == 0) {
				    // we're done emitting the number requested
				    // so return
				    return;
				}
				break;
			    }
			}
		    }
		}
	    }
	}

	@SuppressWarnings("unchecked")
	protected boolean doNext(Object value) {
	    boolean terminal = false;
	    if (value == ON_NEXT_NULL_SENTINEL) {
		super.onNext(null);
	    } else {
		if (value == ON_COMPLETED_SENTINEL) {
		    super.onComplete();
		    terminal = true;
		} else {
		    super.onNext((T) value);
		}
	    }
	    return terminal;
	}
    }

}
