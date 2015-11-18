package it.reactive.muskel.internal.subscriptions;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.internal.SubscriptionTopicMessage;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import org.reactivestreams.Subscription;

@AllArgsConstructor
public class SubscriptionTopic implements Subscription, Serializable {

    private static final long serialVersionUID = 1L;

    protected transient MuskelContext context;

    protected final String subscriberUUID;

    @Override
    public void request(long n) {
	context.publishMessage(subscriberUUID, SubscriptionTopicMessage
		.builder().requestValues(n).build());
    }

    @Override
    public void cancel() {
	context.publishMessage(subscriberUUID, SubscriptionTopicMessage
		.builder().cancel(true).build());

    }
    
}
