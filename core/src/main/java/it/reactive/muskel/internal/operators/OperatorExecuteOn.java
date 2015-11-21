package it.reactive.muskel.internal.operators;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelInjectAware;
import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.publisher.QueuePoolPublisher;
import it.reactive.muskel.internal.subscriber.AbstractSentinelBasedSubscriber;
import it.reactive.muskel.internal.subscriber.subscription.utils.SubscriptionTopicUtils;
import it.reactive.muskel.internal.utils.unsafe.SpscArrayQueue;
import it.reactive.muskel.utils.MuskelExecutorUtils;

import java.io.Serializable;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@AllArgsConstructor
public class OperatorExecuteOn<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;

    private final MuskelContext context;

    @NonNull
    private final MuskelExecutor executor;

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	return executor.isLocal() ? new LocalOperatorExecuteOnSubscriber<>(
		context, t) : new RemoteOperatorExecuteOnSubscriber<>(context,
		t, executor);
    }

    private static class LocalOperatorExecuteOnSubscriber<T> extends
	    AbstractSentinelBasedSubscriber<T, T> {

	private static final long serialVersionUID = 1L;

	private static final int SIZE = 100;

	private final MuskelContext context;

	private final AtomicInteger counter = new AtomicInteger(0);

	private int inQueueCount = 0;

	private final int windowSize;

	private final Queue<Object> queue;

	public LocalOperatorExecuteOnSubscriber(MuskelContext context,
		Subscriber<? super T> t) {
	    this(context, t, SIZE);
	}

	public LocalOperatorExecuteOnSubscriber(MuskelContext context,
		Subscriber<? super T> t, int windowSize) {
	    super(t);
	    this.windowSize = windowSize;
	    //uno in piu' per gestire l'errore o la oncomplete
	    this.queue = new SpscArrayQueue<Object>(windowSize+1);
	    this.context = context;
	}

	@Override
	protected boolean add(Object obj) {
	    return queue.offer(obj);
	}

	@Override
	public void request(long n) {
	    onChildRequestCalled(n);
	}

	@Override
	protected void schedule() {
	    if (counter.getAndIncrement() == 0) {
		context.getMuskelExecutorService().execute(() -> pollQueue(),
			MuskelExecutorUtils.DEFAULT_LOCAL);
	    }
	}

	protected void pollQueue() {

	    do {
		// La variabile inQueueCount e' acceduta da un solo thread e può
		// non essere atomica
		if (inQueueCount > 0) {

		    Object v = queue.poll();
		    onPool(v);
		    if (v != null) {
			inQueueCount--;
		    }
		}
		if (inQueueCount == 0 && !isUnsubscribed()) {
		    //effettuo una nuova richiesta quando la coda e' vuota 
		    long childRequest = childRequested.get();
		    if (childRequest > 0) {
			long totalToRequest = childRequest > windowSize ? windowSize
				: childRequest;
			childRequested.addAndGet(-totalToRequest);
			inQueueCount += totalToRequest;
			super.request(totalToRequest);

		    }
		}
	    } while (counter.decrementAndGet() > 0 && !isUnsubscribed());
	}

    }

    @Slf4j
    private static class RemoteOperatorExecuteOnSubscriber<T> extends
	    AbstractSentinelBasedSubscriber<T, T> {

	private static final long serialVersionUID = 1L;

	private final String subscriberUUID;

	private final MuskelExecutor executor;

	private transient MuskelContext context;

	private transient MuskelQueue<Object> queue;

	public RemoteOperatorExecuteOnSubscriber(MuskelContext context,
		Subscriber<? super T> t, MuskelExecutor executor) {
	    super(t);
	    this.context = context;
	    this.subscriberUUID = context.generateIdentifier();
	    if (executor == null) {
		throw new IllegalArgumentException("Executor cannot be null");
	    }
	    this.executor = executor;
	}

	@Override
	public void onSubscribe(final Subscription s) {
	    // Non invoco la super.onSubscribe perchè deve essere invocato da
	    // MessageListenerCallable
	    try {

		MuskelContext context = getContext();
		SubscriptionTopicUtils.createSubscriptionCallBack(context,
			subscriberUUID, s);

		context.getMuskelExecutorService()
			.submitToKeyOwner(
				new MessageListenerSupplier<>(context,
					subscriberUUID, getChild(), false),
				executor)
			.exceptionally(
				t -> {
				    log.error("Error during execution", t);
				    s.cancel();
				    Optional.ofNullable(getChild()).ifPresent(
					    child -> child.onError(t));
				    return false;
				});
	    } catch (Throwable t) {
		log.error("Error during submit of Execution", t);
		s.cancel();
		Optional.ofNullable(getChild()).ifPresent(
			child -> child.onError(t));

	    }
	}

	@Override
	protected boolean add(Object obj) {
	    if (queue == null) {
		queue = getContext().getQueue(subscriberUUID);
	    }
	    return queue.offer(obj);
	}

	protected MuskelContext getContext() {
	    if (this.context == null) {
		this.context = ThreadLocalMuskelContext.get();
	    }
	    return context;
	}

	@MuskelInjectAware
	protected static class MessageListenerSupplier<T> implements
		Supplier<Boolean>, Serializable {

	    private static final long serialVersionUID = 1L;

	    private Subscriber<? super T> child;

	    private String subscriberUUID;

	    private transient MuskelContext context;

	    private boolean waitTermination;

	    public MessageListenerSupplier(MuskelContext context,
		    String subScriptionName, Subscriber<? super T> child,
		    boolean waitTermination) {
		this.child = child;
		this.subscriberUUID = subScriptionName;
		this.context = context;
		this.waitTermination = waitTermination;
	    }

	    @Override
	    public Boolean get() {

		log.trace("Thread for subscriberUUID = {} started",
			subscriberUUID);
		if (this.context == null) {
		    this.context = ThreadLocalMuskelContext.get();
		}

		final CountDownLatch latch = waitTermination ? new CountDownLatch(
			1) : null;

		try {
		    new QueuePoolPublisher<T>(subscriberUUID, context,
			    ended -> latch.countDown()).subscribe(child);

		    // Devo rimanere in attesa che non ci siano più elementi
		    // nella
		    // lista

		    if (latch != null) {
			try {
			    latch.await();
			} catch (InterruptedException e) {

			}
		    }

		} catch (Throwable t) {

		    log.error("Error executing subscriberUUID "
			    + subscriberUUID, t);
		    throw new RuntimeException(t);
		}
		return null;
	    }

	}

    }

}
