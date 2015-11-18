package it.reactive.muskel;

import it.reactive.muskel.internal.operators.OnSubscribeFromIterable;
import it.reactive.muskel.internal.publisher.ScalarPublisher;
import it.reactive.muskel.processors.GroupedMuskelProcessorImpl;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public interface GroupedMuskelProcessor<K, T> extends MuskelProcessor<T> {

    public static <K, T> GroupedMuskelProcessor<K, T> just(K key, T element) {
	return create(key, new ScalarPublisher<>(element));
    }

    /**
     * Returns an MuskelProcessor that will execute the specified function when
     * a {@link Subscriber} subscribes to it. Write the function you pass to
     * {@code create} so that it behaves as an MuskelProcessor: It should invoke
     * the Subscriber's {@link Subscriber#onNext onNext},
     * {@link Subscriber#onError onError}, and {@link Subscriber#onComplete
     * onComplete} methods appropriately.
     * <p>
     * A well-formed MuskelProcessor must invoke either the Subscriber's
     * {@code onCompleted} method exactly once or its {@code onError} method
     * exactly once.
     * <p>
     * 
     * 
     * @param <K>
     *            the type of the key
     * @param <T>
     *            the type of the items that this MuskelProcessor emits
     * @param key
     *            a key of Group
     * @param f
     *            a function that accepts an {@code Subscriber<T>}, and invokes
     *            its {@code onNext}, {@code onError}, and {@code onCompleted}
     *            methods as appropriate
     * @return a GroupedMuskelProcessor that, when a {@link Subscriber}
     *         subscribes to it, will execute the specified function
     */
    public static <K, T> GroupedMuskelProcessor<K, T> create(K key,
	    Publisher<T> f) {
	return new GroupedMuskelProcessorImpl<K, T>(key, f);
    }

    public static <K, T> GroupedMuskelProcessor<K, T> fromIterable(K key,
	    Iterable<? extends T> elements) {
	return create(key, new OnSubscribeFromIterable<>(elements));
    }

    /**
     * Returns the key that identifies the group of items emited by this
     * {@code GroupedMuskelProcessor}
     * 
     * @return the key that the items emitted by this
     *         {@code GroupedMuskelProcessor} were grouped by
     */
    K getKey();

}
