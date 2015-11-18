package it.reactive.muskel.internal.publisher;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelContextAware;
import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.exceptions.MuskelException;
import it.reactive.muskel.executor.MuskelExecutorService;
import it.reactive.muskel.internal.executor.local.ContextForwardRunnable;
import it.reactive.muskel.internal.operator.utils.BackpressureUtils;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.subscriptions.SubscriptionTopic;
import it.reactive.muskel.internal.utils.RingBuffer;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class QueuePoolPublisher<T> implements Publisher<T>, Serializable,
	MuskelContextAware {

    private static final long serialVersionUID = 1L;

    private transient final Consumer<Boolean> endCallback;

    private transient MuskelContext context;

    private final String subcriptionUUID;

    public QueuePoolPublisher(String subcriptionUUID) {
	this(subcriptionUUID, null, null);
    }

    public QueuePoolPublisher(String subcriptionUUID, MuskelContext context,
	    Consumer<Boolean> endCallback) {
	this.subcriptionUUID = subcriptionUUID;
	this.context = context;
	this.endCallback = endCallback;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
	s.onSubscribe(new QueuePoolEmiterSubscription<T>(context,
		subcriptionUUID, s, endCallback));

    }

    @Override
    public void setContext(MuskelContext context) {
	this.context = context;

    }

    static class QueuePoolEmiterSubscription<T> extends SubscriptionTopic {

	private static final long serialVersionUID = 1L;

	private final AtomicBoolean unsubscribed = new AtomicBoolean();

	private final AtomicBoolean started = new AtomicBoolean();

	private final Subscriber<? super T> s;

	private transient final Consumer<Boolean> endCallback;

	private final AtomicLong outstanding = new AtomicLong();

	private volatile long requested;
	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<QueuePoolEmiterSubscription> REQUESTED = AtomicLongFieldUpdater
		.newUpdater(QueuePoolEmiterSubscription.class, "requested");

	private static final int limit = RingBuffer.SIZE / 4;

	public QueuePoolEmiterSubscription(MuskelContext context,
		String subscriberUUID, Subscriber<? super T> s,
		Consumer<Boolean> endCallback) {
	    super(context, subscriberUUID);
	    this.s = s;
	    this.endCallback = endCallback;
	}

	@Override
	public void request(long n) {
	    if (n < 0) {
		return;
	    }
	    if (requested != Long.MAX_VALUE) {
		BackpressureUtils.getAndAddRequest(REQUESTED, this, n);
	    }
	    requestMore(n);
	    if (started.compareAndSet(false, true)) {

		Optional.ofNullable(context)
			.map(e -> e.getMuskelExecutorService())
			.map(c -> {
			    c.execute(new ContextForwardRunnable(
				    () -> pollQueue(), context), MuskelExecutor
				    .local());
			    return true;
			})
			.orElseThrow(
				() -> new MuskelException(
					"Could not start runnable because Context is null"));

		// new Thread(new ContextForwardRunnable(() -> pollQueue(),
		// context), "QueuePoolEmiter-" + subscriberUUID).start();
	    }
	}

	protected void requestMore(long n) {
	    if (n < 0) {
		return;
	    }
	    long newRequest = outstanding.updateAndGet(old -> Math.min(
		    requested, limit - old));
	    if (newRequest > 0) {
		super.request(newRequest);
	    }
	}

	@Override
	public void cancel() {
	    if (unsubscribed.compareAndSet(false, true)) {
		super.cancel();
	    }

	}

	protected void pollQueue() {
	    boolean finalState = false;
	    final MuskelQueue<Object> queue = context.getQueue(subscriberUUID);
	    while (!unsubscribed.get() && !finalState) {
		Object o;
		try {
		    o = queue.poll(5, TimeUnit.SECONDS);

		    if (requested != Long.MAX_VALUE) {
			BackpressureUtils.getAndAddRequest(REQUESTED, this, -1);
		    }
		    finalState = SentinelUtils.emit(s, o, f -> {
			if (!f)
			    requestMore(requested);
		    });
		} catch (InterruptedException e) {
		    return;
		}

	    }
	    context.closeQueue(subscriberUUID);
	    if (!unsubscribed.get()) {
		context.closeMessageListener(subscriberUUID);
	    }
	    if (endCallback != null) {
		endCallback.accept(finalState);
	    }
	}

    }

}