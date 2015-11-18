package it.reactive.muskel.internal.subscriber;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class AbstractSubscriber<T, K> extends
	AbstractBasicSubscriber<T, K> implements Subscription {

    private static final long serialVersionUID = 1L;

    public AbstractSubscriber() {

    }

    public AbstractSubscriber(Subscriber<? super K> child) {
	super(child);
    }

    protected Subscription doGetSubscription(Subscription s) {
	return this;
    }

    @Override
    public void cancel() {
	unsubscribe();

    }

}
