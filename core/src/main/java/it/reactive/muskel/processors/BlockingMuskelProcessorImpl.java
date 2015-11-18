package it.reactive.muskel.processors;

import it.reactive.muskel.BlockingMuskelProcessor;
import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.functions.SerializablePublisher;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.subscriber.AbstractBasicSubscriber;
import it.reactive.muskel.internal.subscriber.BlockingSubscriber;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscription;

@RequiredArgsConstructor
@Getter
public class BlockingMuskelProcessorImpl<T> implements
	BlockingMuskelProcessor<T> {

    private static final long serialVersionUID = 1L;
    private final @NonNull SerializablePublisher<? extends T> source;

    /**
     * Returns the first item emitted by this {@code BlockingMuskelProcessor},
     * or throws {@code NoSuchElementException} if it emits no items.
     *
     * @return the first item emitted by this {@code BlockingMuskelProcessor}
     * @throws NoSuchElementException
     *             if this {@code BlockingMuskelProcessor} emits no items
     */
    @Override
    public T first() {
	return first(null, true);
    }

    @Override
    public T first(T defaultValue) {
	return first(defaultValue, false);
    }

    protected T first(T defaultValue, boolean thowExceptionNotFound) {
	final AtomicReference<T> returnItem = new AtomicReference<>(
		defaultValue);
	final AtomicReference<Throwable> returnException = new AtomicReference<>();
	final CountDownLatch latch = new CountDownLatch(1);
	final AtomicBoolean foundResult = new AtomicBoolean();
	final AtomicReference<Subscription> subScription = new AtomicReference<>();

	source.subscribe(new AbstractBasicSubscriber<T, T>() {

	    private static final long serialVersionUID = 1L;

	    @Override
	    public void onComplete() {
		latch.countDown();
	    }

	    @Override
	    public void onError(final Throwable e) {
		returnException.set(e);
		latch.countDown();
	    }

	    @Override
	    public void onNext(final T item) {
		returnItem.set(item);
		foundResult.set(true);
	    }

	    @Override
	    public void onSubscribe(Subscription s) {
		subScription.set(s);
		super.onSubscribe(s);
	    }

	});

	try {
	    latch.await();
	} catch (InterruptedException e) {
	    subScription.get().cancel();
	    Thread.currentThread().interrupt();
	    throw new RuntimeException(
		    "Interrupted while waiting for subscription to complete.",
		    e);
	}

	if (returnException.get() != null) {
	    if (returnException.get() instanceof RuntimeException) {
		throw (RuntimeException) returnException.get();
	    } else {
		throw new RuntimeException(returnException.get());
	    }
	}

	if (thowExceptionNotFound && (!foundResult.get())) {
	    throw new NoSuchElementException("No elements Found");
	}
	return returnItem.get();
    }

    public T single() {
	Iterator<T> it = BlockingMuskelProcessor.iterate(MuskelProcessor
		.<T> fromPublisher(source).single());
	return it.next();
    }

    public T single(T defaultValue) {
	Iterator<T> it = BlockingMuskelProcessor.iterate(MuskelProcessor
		.<T> fromPublisher(source).singleOrDefault(defaultValue));
	if (it.hasNext()) {
	    return it.next();
	}
	return defaultValue;
    }

    @Override
    public void close() {
	if (source != null && source instanceof AutoCloseable) {
	    try {
		((AutoCloseable) source).close();
	    } catch (Exception e) {
		throw new RuntimeException(e);
	    }
	}
    }

    /**
     * Subscribes to the source and calls the Subscriber methods on the current
     * thread.
     * <p>
     * The unsubscription and backpressure is composed through.
     * 
     * @param subscriber
     *            the subscriber to forward events and calls to in the current
     *            thread
     */
    @Override
    public void subscribe(SerializableSubscriber<? super T> subscriber) {
	final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

	BlockingSubscriber<T> bs = new BlockingSubscriber<>(queue);

	source.subscribe(bs);

	try {
	    for (;;) {
		if (bs.isCancelled()) {
		    break;
		}
		Object o = queue.poll();
		if (o == null) {
		    if (bs.isCancelled()) {
			break;
		    }
		    o = queue.take();
		}
		if (bs.isCancelled()) {
		    break;
		}
		if (o == BlockingSubscriber.TERMINATED) {
		    break;
		}
		if (SentinelUtils.acceptFull(o, subscriber)) {
		    break;
		}
	    }
	} catch (InterruptedException e) {
	    Thread.currentThread().interrupt();
	    subscriber.onError(e);
	} finally {
	    bs.cancel();
	}
    }

    @Override
    public Iterator<T> iterator() {
	return BlockingMuskelProcessor.iterate(source);
    }

}
