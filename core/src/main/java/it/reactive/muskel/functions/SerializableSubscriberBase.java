package it.reactive.muskel.functions;

import java.io.Serializable;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A serializable base form of {@link Subscriber}
 * 
 * @see Subscriber
 */
public interface SerializableSubscriberBase<T> extends Serializable {

    /**
     * Data notification sent by the {@link Publisher} in response to requests
     * to {@link Subscription#request(long)}.
     * 
     * @param t
     *            the element signaled
     */
    public void onNext(T t);

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)}
     * is invoked again.
     *
     * @param t
     *            the throwable signaled
     */
    public void onError(Throwable t);

    /**
     * Successful terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)}
     * is invoked again.
     */
    public void onComplete();
}
