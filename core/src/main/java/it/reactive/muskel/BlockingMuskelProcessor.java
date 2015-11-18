package it.reactive.muskel;

import it.reactive.muskel.functions.SerializablePublisher;
import it.reactive.muskel.functions.utils.StreamUtils;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.subscriber.LambdaSubscriber;
import it.reactive.muskel.iterators.BlockingIterator;
import it.reactive.muskel.processors.BlockingMuskelProcessorImpl;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * {@code BlockingMuskelProcessor} is a variety of {@link MuskelProcessor} that
 * provides blocking operators. It can be useful for testing and demo purposes,
 * but is generally inappropriate for production applications (if you think you
 * need to use a {@code BlockingMuskelProcessor} this is usually a sign that you
 * should rethink your design).
 * <p>
 * You construct a {@code BlockingMuskelProcessor} from an
 * {@code MuskelProcessor} with {@link #from(BlockingMuskelProcessor)} or
 * {@link MuskelProcessor#toBlocking()}.
 * 
 * @param <T>
 *            the type of item emitted by the {@code BlockingMuskelProcessor}
 */
public interface BlockingMuskelProcessor<T> extends Serializable,
	AutoCloseable, SerializablePublisher<T>, Iterable<T> {

    /**
     * Converts an {@link MuskelProcessor} into a
     * {@code BlockingMuskelProcessor}.
     *
     * @param o
     *            the {@link MuskelProcessor} you want to convert
     * @return a {@code BlockingMuskelProcessor} version of {@code o}
     */
    static <T> BlockingMuskelProcessor<T> from(
	    final SerializablePublisher<? extends T> source) {
	return new BlockingMuskelProcessorImpl<T>(source);
    }

    /**
     * Converts {@link SerializablePublisher} to an {@link Iterator}
     * 
     * @param p
     *            the input {@link SerializablePublisher}
     * @return an {@link Iterator} that can iterate over
     *         {@link SerializablePublisher}
     */
    static <T> BlockingIterator<T> iterate(SerializablePublisher<? extends T> p) {
	final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

	LambdaSubscriber<T> ls = new LambdaSubscriber<>(
		v -> queue.offer(SentinelUtils.next(v)),
		e -> queue.offer(SentinelUtils.error(e)),
		() -> queue.offer(SentinelUtils.complete()),
		s -> s.request(Long.MAX_VALUE));

	p.subscribe(ls);

	return new BlockingIterator<>(queue, ls);
    }

    @Override
    void close();

    /**
     * Returns a sequential {@code Stream} with this
     * {@code BlockingMuskelProcessor} as its source.
     *
     * @return a sequential {@code Stream} over the elements in this
     *         {@code BlockingMuskelProcessor}
     */
    default Stream<T> stream() {
	return StreamUtils.makeStream(iterator(), false);
    }

    /**
     * Returns a possibly parallel {@code Stream} with this
     * {@code BlockingMuskelProcessor} as its source. It is allowable for this
     * method to return a sequential stream.
     *
     * @return a possibly parallel {@code Stream} over the elements in this
     *         {@code BlockingMuskelProcessor}
     */
    default Stream<T> parallelStream() {
	return StreamUtils.makeStream(iterator(), true);
    }

    /**
     * Returns the first item emitted by this {@code BlockingMuskelProcessor},
     * or throws {@code NoSuchElementException} if it emits no items.
     *
     * @return the first item emitted by this {@code BlockingMuskelProcessor}
     * @throws NoSuchElementException
     *             if this {@code BlockingMuskelProcessor} emits no items
     */
    T first();

    /**
     * Returns the first item emitted by this {@code BlockingMuskelProcessor},
     * or the defaultValue if it emits no items.
     * 
     * @param defaultValue
     *            the defaultValue
     * @return the first item emitted by this {@code BlockingMuskelProcessor}
     */
    T first(T defaultValue);

    /**
     * Returns the last element emitted by this {@code BlockingMuskelProcessor}
     * 
     * @return the last element of @code BlockingMuskelProcessor}
     * 
     */
    default Optional<T> lastOption() {
	return stream().reduce((a, b) -> b);
    }

    /**
     * Returns the last element emitted by this {@code BlockingMuskelProcessor}
     * 
     * @return the last element of @code BlockingMuskelProcessor}
     * @throws NoSuchElementException
     *             if {@code BlockingMuskelProcessor} countains no elements
     */
    default T last() {
	Optional<T> o = lastOption();
	if (o.isPresent()) {
	    return o.get();
	}
	throw new NoSuchElementException("No elements found");
    }

    /**
     * Returns the last element emitted by this {@code BlockingMuskelProcessor}
     * or the default value is the {@code BlockingMuskelProcessor} have no
     * elements
     * 
     * @return the last element of @code BlockingMuskelProcessor}
     * 
     * @param defaultValue
     *            the defaultValue
     */
    default T last(T defaultValue) {
	Optional<T> o = lastOption();
	if (o.isPresent()) {
	    return o.get();
	}
	return defaultValue;
    }

    /**
     * Returns the first item emitted by the source
     * {@code BlockingMuskelProcessor}, if that {@code BlockingMuskelProcessor}
     * emits only a single item. If the source {@code BlockingMuskelProcessor}
     * emits more than one item or no items, notify of an
     * {@code IllegalArgumentException} or {@code NoSuchElementException}
     * respectively.
     * 
     * @return the first items emitted by the source
     *         {@code BlockingMuskelProcessor}
     * @throws IllegalArgumentException
     *             if the source emits more than one item
     * @throws NoSuchElementException
     *             if the source emits no items
     */
    T single();

    /**
     * Returns the first item emitted by the source
     * {@code BlockingMuskelProcessor}, if that {@code BlockingMuskelProcessor}
     * emits only a single item, or a default item if the source
     * {@code BlockingMuskelProcessor} emits no items. If the source
     * {@code BlockingMuskelProcessor} emits more than one item, throw an
     * {@code IllegalArgumentException}.
     * 
     * @param defaultValue
     *            a default value to emit if the source
     *            {@code BlockingMuskelProcessor} emits no item
     * @return the first items emitted by the source
     *         {@code BlockingMuskelProcessor}
     * @throws IllegalArgumentException
     *             if the source MuskelProcessor emits more than one item
     */
    T single(T defaultValue);

    @Override
    default void forEach(Consumer<? super T> action) {
	Iterator<T> it = iterator();
	while (it.hasNext()) {
	    try {
		action.accept(it.next());
	    } catch (Throwable e) {
		if (it instanceof AutoCloseable) {
		    try {
			((AutoCloseable) it).close();
		    } catch (Exception e1) {
		    }
		}

		throw e;
	    }
	}
    }

}
