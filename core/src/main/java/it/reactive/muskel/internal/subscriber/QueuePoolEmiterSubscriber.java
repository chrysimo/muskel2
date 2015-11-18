package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.internal.executor.local.ContextForwardRunnable;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.reactivestreams.Subscriber;

@Deprecated
public class QueuePoolEmiterSubscriber<T, K> extends AbstractSubscriber<T, K> {

    private static final long serialVersionUID = 1L;

    private final Consumer<Boolean> endCallback;

    private final MuskelQueue<Object> queue;

    public QueuePoolEmiterSubscriber(Subscriber<? super K> child,
	    MuskelQueue<Object> queue) {
	this(child, queue, null, false);
    }

    public QueuePoolEmiterSubscriber(Subscriber<? super K> child,
	    MuskelQueue<Object> queue, Consumer<Boolean> endCallback) {
	this(child, queue, endCallback, false);
    }

    public QueuePoolEmiterSubscriber(Subscriber<? super K> child,
	    MuskelQueue<Object> queue, Consumer<Boolean> endCallback,
	    boolean autostart) {
	super(child);
	this.endCallback = endCallback;
	this.queue = queue;
	if (autostart) {
	    startThreadIfNeeded();
	}
    }

    private final AtomicBoolean threadStarted = new AtomicBoolean();

    @Override
    public void request(long n) {
	if (n > 0) {

	    startThreadIfNeeded();
	    super.request(n);
	}
    }

    protected boolean startThreadIfNeeded() {
	boolean result = threadStarted.compareAndSet(false, true);

	if (result) {
	    new Thread(new ContextForwardRunnable(() -> pollQueue()),
		    "MuskelQueuePoolEmiterSubscriber-" + queue).start();

	}
	return result;
    }

    protected void pollQueue() {
	boolean finalState = false;
	while (!isUnsubscribed() && !finalState) {
	    Object o;
	    try {
		o = queue.poll(5, TimeUnit.SECONDS);
		finalState = SentinelUtils.emit(this, o);
	    } catch (InterruptedException e) {
		return;
	    }

	}
	if (endCallback != null) {
	    endCallback.accept(finalState);
	}
    }
}