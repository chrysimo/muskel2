package it.reactive.muskel.internal.operators;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelContextAware;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.functions.SerializableBiFunction;
import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.functions.SerializableSupplier;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.operator.utils.BackpressureUtils;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;
import it.reactive.muskel.utils.MuskelExecutorUtils;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@AllArgsConstructor
public class OperatorMap<T, R> implements Operator<R, T> {

    private static final long serialVersionUID = 1L;
    @NonNull
    private final SerializableFunction<? super T, ? extends R> transformer;

    private transient final MuskelContext context;

    private final MuskelExecutor executor;

    @Override
    public Subscriber<? super T> apply(final Subscriber<? super R> o) {
	return executor == null ? new OperatorMapSubscribe<>(transformer, o)
		: new OperatorMapSubscribeExecutor<>(transformer, o, context,
			executor);
    }

    @RequiredArgsConstructor
    private static class OperatorMapSubscribe<T, R> implements SerializableSubscriber<T> {
	private static final long serialVersionUID = 1L;

	@NonNull
	private SerializableFunction<? super T, ? extends R> transformer;
	@NonNull
	private final Subscriber<? super R> o;

	@Override
	public void onError(Throwable e) {
	    o.onError(e);
	}

	@Override
	public void onSubscribe(Subscription s) {
	    this.transformer = ManagedContextUtils
		    .tryInitialize(s, transformer);
	    o.onSubscribe(s);
	}

	@Override
	public void onNext(T t) {
	    try {
		o.onNext(transformer.apply(t));
	    } catch (Throwable e) {
		onError(e);
	    }
	}

	@Override
	public void onComplete() {
	    o.onComplete();

	}
    }

    private static class OperatorMapSubscribeExecutor<T, R> extends
	    AbstractSubscriber<T, R> {
	private static final long serialVersionUID = 1L;

	private final SerializableFunction<? super T, ? extends R> transformer;

	private transient MuskelContext context;

	private final MuskelExecutor executor;

	private transient CompletableFuture<Void> prec;

	private volatile long requested = 0;
	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<OperatorMapSubscribeExecutor> REQUESTED = AtomicLongFieldUpdater
		.newUpdater(OperatorMapSubscribeExecutor.class, "requested");

	private volatile long inQueueCount;

	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<OperatorMapSubscribeExecutor> IN_QUEUE_COUNT = AtomicLongFieldUpdater
		.newUpdater(OperatorMapSubscribeExecutor.class, "inQueueCount");

	@SuppressWarnings("unused")
	private volatile long counter;

	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<OperatorMapSubscribeExecutor> COUNTER_UPDATER = AtomicLongFieldUpdater
		.newUpdater(OperatorMapSubscribeExecutor.class, "counter");

	private final int windowSize = 100;

	private final SerializableFunction<Object, Object> enqueueFunction = f -> {
	    enqueue();
	    return f;
	};

	private final Runnable requestMoreRunnable = (Runnable & Serializable) () -> dequeueAndRequestMore();

	private final SerializableBiFunction<Object, Throwable, Object> sentilelTransformer = (
		k, e) -> e != null ? SentinelUtils.error(e) : k;

	public OperatorMapSubscribeExecutor(
		SerializableFunction<? super T, ? extends R> transformer,
		Subscriber<? super R> o, MuskelContext context,
		MuskelExecutor executor) {
	    super(o);
	    this.transformer = transformer;
	    this.context = context;
	    this.executor = executor;
	}

	private void dequeueAndRequestMore() {
	    long newRequest;
	    do {
		counter = 1;
		while ((newRequest = (Math.min(windowSize, requested) - inQueueCount)) > 0) {

		    IN_QUEUE_COUNT.addAndGet(this, newRequest);

		    if (requested != Long.MAX_VALUE) {
			BackpressureUtils.getAndAddRequest(REQUESTED, this,
				-newRequest);
		    }

		    if (isUnsubscribed()) {
			return;
		    }
		    super.request(newRequest);

		}

	    } while (COUNTER_UPDATER.decrementAndGet(this) > 0);

	}

	private void schedule() {

	    if (context == null) {
		context = ThreadLocalMuskelContext.get();
	    }

	    if (context != null) {

		if (COUNTER_UPDATER.getAndIncrement(this) == 0) {
		    try {
			context.getMuskelExecutorService().execute(
				requestMoreRunnable,
				MuskelExecutorUtils.DEFAULT_LOCAL);
		    } catch (Throwable t) {
			cancel();
			super.onError(t);
		    }
		}

	    } else {
		throw new IllegalArgumentException("Context cannot be null");
	    }
	}

	private void enqueue() {
	    IN_QUEUE_COUNT.decrementAndGet(this);
	    schedule();
	}

	@Override
	public void request(long n) {
	    BackpressureUtils.getAndAddRequest(REQUESTED, this, n);
	    schedule();
	}

	@Override
	public void onError(Throwable e) {
	    this.getPreviousTask().thenRun(() -> super.onError(e));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onNext(T t) {
	    if (!isUnsubscribed()) {
		try {

		    CompletableFuture<Object> futureResult = context
			    .getMuskelExecutorService()
			    .submitToKeyOwner(
				    new TransformerSupplier<>(t, transformer,
					    context), executor)
			    .thenApply(enqueueFunction)
			    .handle(sentilelTransformer);

		    this.prec = getPreviousTask().runAfterBoth(
			    futureResult,
			    () -> {
				if (!isUnsubscribed()) {
				    try {
					Object result = futureResult.get();
					if (SentinelUtils.isError(result)) {
					    cancel();
					    super.onError(SentinelUtils
						    .getError(result));

					} else {

					    doOnNext((R) result);
					}
				    } catch (Throwable e) {
					cancel();
					super.onError(e);

				    }
				}
			    });
		} catch (Throwable e) {
		    onError(e);
		}
	    }
	}

	@Override
	public void onComplete() {
	    this.getPreviousTask().thenRun(() -> super.onComplete());
	}

	protected CompletableFuture<Void> getPreviousTask() {
	    return Optional.ofNullable(prec).orElseGet(
		    () -> CompletableFuture.completedFuture(null));
	}

    }

    @AllArgsConstructor
    @NoArgsConstructor
    private static class TransformerSupplier<T, R> implements
	    SerializableSupplier<R>, MuskelContextAware {

	private static final long serialVersionUID = 1L;

	private T source;

	private SerializableFunction<? super T, ? extends R> transformer;

	private transient MuskelContext context;

	@Override
	public R get() {

	    R result = ManagedContextUtils.tryInitialize(context, transformer)
		    .apply(source);
	    return result;

	}

	@Override
	public void setContext(MuskelContext context) {
	    this.context = context;

	}
    }
}
