package it.reactive.muskel.internal.subscriptions;

import java.io.Serializable;

import org.reactivestreams.Subscription;

public class ArbiterSubscription implements Subscription, Serializable {

    private static final long serialVersionUID = 1L;
    long requested;
    Subscription currentSubscription;

    boolean emitting;
    long missedRequested;
    long missedSubscripted;
    Subscription missedSubscription;

    static final Subscription NULL_PRODUCER = new Subscription() {
	@Override
	public void request(long n) {

	}

	@Override
	public void cancel() {
	    // TODO Auto-generated method stub

	}
    };

    @Override
    public void request(long n) {
	if (n < 0) {
	    throw new IllegalArgumentException("n >= 0 required");
	}
	if (n == 0) {
	    return;
	}
	synchronized (this) {
	    if (emitting) {
		missedRequested += n;
		return;
	    }
	    emitting = true;
	}
	boolean skipFinal = false;
	try {
	    long r = requested;
	    long u = r + n;
	    if (u < 0) {
		u = Long.MAX_VALUE;
	    }
	    requested = u;

	    Subscription p = currentSubscription;
	    if (p != null) {
		p.request(n);
	    }

	    emitLoop();
	    skipFinal = true;
	} finally {
	    if (!skipFinal) {
		synchronized (this) {
		    emitting = false;
		}
	    }
	}
    }

    public void produced(long n) {
	if (n <= 0) {
	    throw new IllegalArgumentException("n > 0 required");
	}
	synchronized (this) {
	    if (emitting) {
		missedSubscripted += n;
		return;
	    }
	    emitting = true;
	}

	boolean skipFinal = false;
	try {
	    long r = requested;
	    if (r != Long.MAX_VALUE) {
		long u = r - n;
		if (u < 0) {
		    throw new IllegalStateException(
			    "more items arrived than were requested");
		}
		requested = u;
	    }

	    emitLoop();
	    skipFinal = true;
	} finally {
	    if (!skipFinal) {
		synchronized (this) {
		    emitting = false;
		}
	    }
	}
    }

    public void setSubscription(Subscription newSubscription) {
	synchronized (this) {
	    if (emitting) {
		missedSubscription = newSubscription == null ? NULL_PRODUCER
			: newSubscription;
		return;
	    }
	    emitting = true;
	}
	boolean skipFinal = false;
	try {
	    currentSubscription = newSubscription;
	    if (newSubscription != null) {
		newSubscription.request(requested);
	    }

	    emitLoop();
	    skipFinal = true;
	} finally {
	    if (!skipFinal) {
		synchronized (this) {
		    emitting = false;
		}
	    }
	}
    }

    public void emitLoop() {
	for (;;) {
	    long localRequested;
	    long localSubscripted;
	    Subscription localProducer;
	    synchronized (this) {
		localRequested = missedRequested;
		localSubscripted = missedSubscripted;
		localProducer = missedSubscription;
		if (localRequested == 0L && localSubscripted == 0L
			&& localProducer == null) {
		    emitting = false;
		    return;
		}
		missedRequested = 0L;
		missedSubscripted = 0L;
		missedSubscription = null;
	    }

	    long r = requested;

	    if (r != Long.MAX_VALUE) {
		long u = r + localRequested;
		if (u < 0 || u == Long.MAX_VALUE) {
		    r = Long.MAX_VALUE;
		    requested = r;
		} else {
		    long v = u - localSubscripted;
		    if (v < 0) {
			throw new IllegalStateException(
				"more subscripted than requested");
		    }
		    r = v;
		    requested = v;
		}
	    }
	    if (localProducer != null) {
		if (localProducer == NULL_PRODUCER) {
		    currentSubscription = null;
		} else {
		    currentSubscription = localProducer;
		    localProducer.request(r);
		}
	    } else {
		Subscription p = currentSubscription;
		if (p != null && localRequested != 0L) {
		    p.request(localRequested);
		}
	    }
	}
    }

    @Override
    public void cancel() {
	if (currentSubscription != null) {
	    currentSubscription.cancel();
	}

    }

}
