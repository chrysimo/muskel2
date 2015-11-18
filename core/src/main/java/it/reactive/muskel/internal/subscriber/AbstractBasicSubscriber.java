package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.functions.SerializableSubscriber;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class AbstractBasicSubscriber<T, K> implements
	SerializableSubscriber<T> {

    private static final Long NOT_SET = Long.MIN_VALUE;

    private static final long serialVersionUID = 1L;

    protected final Subscriber<? super K> child;

    private long requested = NOT_SET;

    private Subscription subscription;

    private volatile boolean unsubscribed;

    public AbstractBasicSubscriber() {
	this(null);
    }

    public AbstractBasicSubscriber(Subscriber<? super K> child) {
	this.child = child;
    }

    protected Subscription getSubscription() {
	return this.subscription;
    }

    protected Subscriber<? super K> getChild() {
	return this.child;
    }

    @Override
    public void onSubscribe(Subscription s) {

	if (this.subscription == null) {
	    long toRequest = requested;
	    this.subscription = s;
	    if (child != null) {
		this.child.onSubscribe(doGetSubscription(s));
	    } else {
		if (toRequest == Long.MIN_VALUE) {
		    request(Long.MAX_VALUE, true);
		} else {
		    request(toRequest, true);
		}
	    }
	} else {
	    throw new IllegalArgumentException("onSubscribe already called");
	}

    }

    public void unsubscribe() {
	this.unsubscribed = true;
	Subscription subscription = null;
	if (!unsubscribed) {
	    synchronized (this) {
		if (!unsubscribed) {
		    subscription = this.subscription;
		}
	    }
	}
	// Lo invoco al di fuori del blocco sincronized
	if (subscription != null) {
	    subscription.cancel();
	}

    }

    public boolean isUnsubscribed() {
	return this.unsubscribed;
    }

    protected Subscription doGetSubscription(Subscription s) {
	return s;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(T t) {
	doOnNext((K) t);
    }

    public void doOnNext(K t) {
	if (child != null) {
	    this.child.onNext(t);
	}
    }

    @Override
    public void onError(Throwable t) {
	if (child != null) {
	    this.child.onError(t);
	}
    }

    @Override
    public void onComplete() {
	if (child != null) {
	    this.child.onComplete();
	}
    }

    public void request(long n) {
	request(n, false);
    }

    protected void request(long n, boolean force) {
	if (n < 0) {
	    throw new IllegalArgumentException(
		    "number requested cannot be negative: " + n);
	}

	// if producer is set then we will request from it
	// otherwise we increase the requested count by n
	Subscription target = null;
	if (force || requested != Long.MAX_VALUE) {
	    synchronized (this) {
		boolean subscriptionNull = subscription == null;
		if (subscriptionNull || n == Long.MAX_VALUE) {
		    addToRequested(n);
		}
		if (!subscriptionNull) {
		    target = subscription;
		}
	    }
	}
	// after releasing lock (we should not make requests holding a lock)
	if (target != null) {
	    target.request(n);
	}
    }

    private void addToRequested(long n) {
	if (requested == NOT_SET) {
	    requested = n;
	} else {
	    final long total = requested + n;
	    // check if overflow occurred
	    if (total < 0) {
		requested = Long.MAX_VALUE;
	    } else {
		requested = total;
	    }
	}
    }
}
