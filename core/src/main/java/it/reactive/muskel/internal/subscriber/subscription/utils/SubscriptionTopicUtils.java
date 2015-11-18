package it.reactive.muskel.internal.subscriber.subscription.utils;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.internal.SubscriptionTopicMessage;

import java.util.concurrent.atomic.AtomicReference;

import lombok.experimental.UtilityClass;

import org.reactivestreams.Subscription;

@UtilityClass
public class SubscriptionTopicUtils {

    public static String createSubscriptionCallBack(MuskelContext context,
	    String subscriberUUID, final Subscription s) {
	final AtomicReference<String> registrationId = new AtomicReference<>();

	registrationId
		.set(context
			.addMessageListener(
				subscriberUUID,
				message -> {

				    SubscriptionTopicMessage event = (SubscriptionTopicMessage) message
					    .getMessageObject();
				    if (event != null) {
					if (event.getCancel() != null
						&& event.getCancel()) {
					    s.cancel();
					    context.removeMessageListener(
						    subscriberUUID,
						    registrationId.get());
					    context.closeMessageListener(subscriberUUID);
					} else {
					    if (event.getRequestValues() != null) {
						Long value = event
							.getRequestValues();
						s.request(value);
					    }
					}
				    }

				}));
	return registrationId.get();

    }

}
