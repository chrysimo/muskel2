package it.reactive.muskel.internal.operators;

import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.operator.utils.BackpressureUtils;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;
import it.reactive.muskel.internal.subscriptions.ArbiterSubscription;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class OperatorConcat<T> implements
	Operator<T, MuskelProcessor<? extends T>> {

    private static final long serialVersionUID = 1L;

    private static final class Holder {
	/** A singleton instance. */
	static final OperatorConcat<Object> INSTANCE = new OperatorConcat<Object>();
    }

    /**
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorConcat<T> instance() {
	return (OperatorConcat<T>) Holder.INSTANCE;
    }

    @Override
    public Subscriber<? super MuskelProcessor<? extends T>> apply(
	    Subscriber<? super T> t) {

	// final SerializedSubscriber<T> s = new SerializedSubscriber<T>(t);
	// final SerialSubscription current = new SerialSubscription();
	// child.add(current);
	ConcatSubscriber<T> cs = new ConcatSubscriber<T>(t);
	// ConcatSubscription<T> cp = new ConcatSubscription<T>(cs);

	// t.onSubscribe(cp);

	return cs;

    }

    static final class ConcatSubscription<T> implements Subscription {
	final ConcatSubscriber<T> cs;

	private boolean first = true;

	ConcatSubscription(ConcatSubscriber<T> cs) {
	    this.cs = cs;
	}

	@Override
	public void request(long n) {
	    if (first) {
		first = false;
		cs.request(2);
	    }
	    cs.requestFromChild(n);
	}

	@Override
	public void cancel() {
	    cs.queue.clear();
	    cs.arbiter.cancel();

	}

    }

    static final class ConcatSubscriber<T> extends
	    AbstractSubscriber<MuskelProcessor<? extends T>, T> {

	private static final long serialVersionUID = 1L;

	// private final SerialSubscription current;
	final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();

	volatile ConcatInnerSubscriber<T> currentSubscriber;

	volatile int wip;
	@SuppressWarnings("rawtypes")
	static final AtomicIntegerFieldUpdater<ConcatSubscriber> WIP = AtomicIntegerFieldUpdater
		.newUpdater(ConcatSubscriber.class, "wip");

	// accessed by REQUESTED
	private volatile long requested;
	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<ConcatSubscriber> REQUESTED = AtomicLongFieldUpdater
		.newUpdater(ConcatSubscriber.class, "requested");
	private final ArbiterSubscription arbiter = new ArbiterSubscription();

	public ConcatSubscriber(Subscriber<? super T> child) {
	    super(child);

	}

	private boolean first = true;

	public void onSubscibe(Subscription s) {
	    super.onSubscribe(s);
	}

	@Override
	public void request(long n) {
	    if (first) {
		first = false;
		super.request(2);
	    }
	    requestFromChild(n);
	}

	@Override
	public void cancel() {
	    queue.clear();
	    arbiter.cancel();

	}

	private void requestFromChild(long n) {
	    if (n <= 0)
		return;
	    // we track 'requested' so we know whether we should subscribe the
	    // next or not
	    long previous = BackpressureUtils.getAndAddRequest(REQUESTED, this,
		    n);
	    arbiter.request(n);
	    if (previous == 0) {
		if (currentSubscriber == null && wip > 0) {
		    // this means we may be moving from one subscriber to
		    // another after having stopped processing
		    // so need to kick off the subscribe via this request
		    // notification
		    subscribeNext();
		}
	    }
	}

	private void decrementRequested() {
	    REQUESTED.decrementAndGet(this);
	}

	@Override
	public void onNext(MuskelProcessor<? extends T> t) {
	    queue.add(SentinelUtils.next(t));
	    if (WIP.getAndIncrement(this) == 0) {
		subscribeNext();
	    }
	}

	@Override
	public void onError(Throwable e) {
	    child.onError(e);
	    unsubscribe();
	}

	@Override
	public void onComplete() {
	    queue.add(SentinelUtils.complete());
	    if (WIP.getAndIncrement(this) == 0) {
		subscribeNext();
	    }
	}

	void completeInner() {
	    currentSubscriber = null;
	    if (WIP.decrementAndGet(this) > 0) {
		subscribeNext();
	    }
	    super.request(1);
	}

	void subscribeNext() {
	    if (requested > 0) {
		Object o = queue.poll();
		if (SentinelUtils.isComplete(o)) {
		    child.onComplete();
		} else if (o != null) {
		    MuskelProcessor<? extends T> obs = SentinelUtils
			    .getValue(o);
		    currentSubscriber = new ConcatInnerSubscriber<T>(this,
			    child, arbiter);

		    // current.set(currentSubscriber);
		    obs.subscribe(currentSubscriber);
		}
	    } else {
		// requested == 0, so we'll peek to see if we are completed,
		// otherwise wait until another request
		Object o = queue.peek();
		if (SentinelUtils.isComplete(o)) {
		    child.onComplete();
		}
	    }
	}
    }

    private static class ConcatInnerSubscriber<T> implements
	    SerializableSubscriber<T> {

	private static final long serialVersionUID = 1L;
	private final Subscriber<? super T> child;
	private final ConcatSubscriber<T> parent;
	@SuppressWarnings("unused")
	private volatile int once = 0;
	@SuppressWarnings("rawtypes")
	private final static AtomicIntegerFieldUpdater<ConcatInnerSubscriber> ONCE = AtomicIntegerFieldUpdater
		.newUpdater(ConcatInnerSubscriber.class, "once");
	private final ArbiterSubscription arbiter;

	public ConcatInnerSubscriber(ConcatSubscriber<T> parent,
		Subscriber<? super T> child, ArbiterSubscription arbiter) {
	    this.parent = parent;
	    this.child = child;
	    this.arbiter = arbiter;
	}

	@Override
	public void onNext(T t) {
	    child.onNext(t);
	    parent.decrementRequested();
	    arbiter.produced(1);
	}

	@Override
	public void onError(Throwable e) {
	    if (ONCE.compareAndSet(this, 0, 1)) {
		// terminal error through parent so everything gets cleaned up,
		// including this inner
		parent.onError(e);
	    }
	}

	@Override
	public void onComplete() {
	    if (ONCE.compareAndSet(this, 0, 1)) {
		// terminal completion to parent so it continues to the next
		parent.completeInner();
	    }
	}

	@Override
	public void onSubscribe(Subscription s) {
	    arbiter.setSubscription(s);

	}

    };

}
