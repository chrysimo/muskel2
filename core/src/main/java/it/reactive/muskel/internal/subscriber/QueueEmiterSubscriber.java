package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.internal.subscriber.subscription.utils.SubscriptionTopicUtils;

import java.util.Optional;

import lombok.Getter;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class QueueEmiterSubscriber<T> extends
	AbstractSentinelBasedSubscriber<T, T> {

    private static final long serialVersionUID = 1L;

    @Getter
    protected final String subcriptionUUID;

    private transient MuskelQueue<Object> queue;

    protected transient MuskelContext context;

    private String subscriptionCallback;

    public QueueEmiterSubscriber(MuskelContext context,
	    Subscriber<? super T> child) {
	super(child);
	this.context = context;
	this.subcriptionUUID = context.generateIdentifier();
    }

    @Override
    protected boolean add(Object obj) {
	return Optional.ofNullable(queue).orElseGet(() -> buildQueue())
		.offer(obj);
    }

    @Override
    public void onComplete() {
	super.onComplete();
	if (subscriptionCallback != null) {
	    this.getContext().removeMessageListener(subcriptionUUID,
		    this.subscriptionCallback);
	}
    }

    @Override
    public void onSubscribe(final Subscription s) {
	this.subscriptionCallback = SubscriptionTopicUtils
		.createSubscriptionCallBack(getContext(), subcriptionUUID, s);

    }

    protected MuskelQueue<Object> buildQueue() {
	this.queue = getContext().getQueue(subcriptionUUID);
	return queue;
    }

    protected MuskelContext getContext() {
	if (this.context == null) {
	    this.context = ThreadLocalMuskelContext.get();
	}
	return context;
    }

}