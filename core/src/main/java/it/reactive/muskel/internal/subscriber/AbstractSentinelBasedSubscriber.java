package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.internal.operator.utils.SentinelUtils;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class AbstractSentinelBasedSubscriber<T, K> extends
	AbstractSubscriber<T, K> {

    private static final long serialVersionUID = 1L;

    protected final AtomicLong childRequested = new AtomicLong(Long.MIN_VALUE);

    protected final Subscriber<T> superSubscriber = new Subscriber<T>() {

	@Override
	public void onSubscribe(Subscription s) {
	    AbstractSentinelBasedSubscriber.super.onSubscribe(s);

	}

	@Override
	public void onNext(T t) {
	    AbstractSentinelBasedSubscriber.super.onNext(t);

	}

	@Override
	public void onError(Throwable t) {
	    AbstractSentinelBasedSubscriber.super.onError(t);

	}

	@Override
	public void onComplete() {
	    AbstractSentinelBasedSubscriber.super.onComplete();

	}
    };

    public AbstractSentinelBasedSubscriber(Subscriber<? super K> t) {
	super(t);

    }

    protected void onChildRequestCalled(long n) {
	if (n > 0) {
	    childRequested.updateAndGet(operand -> {
		long result = operand;
		if (operand == Long.MIN_VALUE) {
		    result = n;
		} else {
		    if (operand < Long.MAX_VALUE) {
			final long total = operand + n;
			// check if overflow occurred
		    if (total < 0) {
			result = Long.MAX_VALUE;
		    } else {
			result = total;
		    }
		}
	    }
	    return result;

	})  ;
	    schedule();
	}
    }

    @Override
    public void onNext(final T t) {
	boolean success = add(SentinelUtils.getValue(t));
	if (!success) {
	    // TODO schedule onto inner after clearing the queue and
	    // cancelling existing work
	    super.onError(new IllegalStateException(
		    "Unable to queue onNext. Backpressure request ignored."));
	    return;
	}
	schedule();
    }

    @Override
    public void onComplete() {
	if (!add(SentinelUtils.complete())) {
	    super.onError(new IllegalStateException(
		    "Unable to add onCompleted . Backpressure request ignored."));
	}
	schedule();
    }

    @Override
    public void onError(final Throwable e) {
	unsubscribe(); // unsubscribe upwards to shut down (do this here so
		       // we don't have delay across threads of final
		       // SafeSubscriber doing this)

	// existing work
	if (!add(SentinelUtils.error(e))) {
	    super.onError(new IllegalStateException(
		    "Unable to add onError . Backpressure request ignored."));
	}
	schedule();
    }

    protected abstract boolean add(Object obj);

    protected void schedule() {

    }

    protected boolean onPool(Object v) {

	return SentinelUtils.emit(superSubscriber, v);

    }

}
