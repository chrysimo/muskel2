package it.reactive.muskel.processors;

import java.util.concurrent.atomic.AtomicReference;

import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.proxy.ProxyMuskelContext;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractBasicSubscriber;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MuskelProcessorImpl<T> implements MuskelProcessor<T> {
    private static final long serialVersionUID = 1L;

    private final Publisher<T> publisher;

    private transient MuskelContext context;

    /**
     * Creates a MuskelProcessor with a {@link Publisher} to execute when it is
     * subscribed to.
     * <p>
     * <em>Note:</em> Use {@link #create(Publisher)} to create an
     * MuskelProcessor, instead of this constructor, unless you specifically
     * have a need for inheritance.
     * 
     * @param publisher
     *            {@link Publisher} to be executed when
     *            {@link #subscribe(Subscriber)} is called
     */
    public MuskelProcessorImpl(Publisher<T> publisher) {
	this(publisher, new ProxyMuskelContext());
    }

    /**
     * Creates a MuskelProcessor with a {@link Publisher} to execute when it is
     * subscribed to and a {@link MuskelContext}.
     * <p>
     * <em>Note:</em> Use {@link #create(Publisher)} to create an
     * MuskelProcessor, instead of this constructor, unless you specifically
     * have a need for inheritance.
     * 
     * @param publisher
     *            {@link Publisher} to be executed when
     *            {@link #subscribe(Subscriber)} is called
     * @param context
     *            {@link MuskelContext} for thread pooling
     * 
     */
    public MuskelProcessorImpl(Publisher<T> publisher, MuskelContext context) {
	this.publisher = publisher;
	this.context = context;
    }

    @Override
    public MuskelProcessor<T> withContext(MuskelContext context) {
	if (this.context instanceof ProxyMuskelContext) {
	    ((ProxyMuskelContext) this.context).setTarget(context);
	} else {
	    this.context = context;
	}
	return this;
    }

    @Override
    public void close() {
	if (this.context != null) {
	    this.context.close();
	}

    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> MuskelProcessor<R> lift(
	    final Operator<? extends R, ? super T> lift) {
	return (MuskelProcessor<R>) MuskelProcessor.create(s -> {
	    try {
		Subscriber<? super T> st = lift.apply(s);
		publisher.subscribe(st);

	    } catch (Throwable t) {
		s.onError(t);
	    }

	}, getContext());

    }

    @Override
    public void subscribe(SerializableSubscriber<? super T> subscriber) {
	// validate and proceed
	if (subscriber == null) {
	    throw new IllegalArgumentException("observer can not be null");
	}
	if (publisher == null) {
	    throw new IllegalStateException(
		    "onSubscribe function can not be null.");

	}
	AtomicReference<Subscription> subscription = new AtomicReference<Subscription>();

	// new Subscriber so onStart it
	publisher.subscribe(new AbstractBasicSubscriber<T, T>(subscriber) {

	    private static final long serialVersionUID = 1L;

	    @Override
	    public void onSubscribe(Subscription s) {
		// Puo essere nullo a seguito della serializzazione
		if (subscription != null) {
		    subscription.set(s);
		}
		super.onSubscribe(s);
	    }

	});

	// return subscription.get();
    }

    @Override
    public MuskelContext getContext() {
	return this.context;
    }

}
