package it.reactive.muskel.functions;

import org.reactivestreams.Subscriber;

/**
 * A serializable form of {@link Subscriber}
 * 
 * @see Subscriber
 */
public interface SerializableSubscriber<T> extends Subscriber<T>,
	SerializableSubscriberBase<T> {

}
