package it.reactive.muskel.internal.operators;

import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.functions.SerializableBiFunction;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.functions.SerializableSupplier;
import it.reactive.muskel.internal.functions.Operator;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
public class OperatorScan<R, T> implements Operator<R, T> {

    private static final long serialVersionUID = 1L;

    private static final Object NO_INITIAL_VALUE = new ImmutableObject();

    @NonNull
    private final SerializableSupplier<R> initialValueFunction;

    @NonNull
    private final SerializableBiFunction<R, ? super T, R> accumulator;

    @SuppressWarnings("unchecked")
    public OperatorScan(SerializableBiFunction<R, ? super T, R> accumulator) {
	this((R) NO_INITIAL_VALUE, accumulator);
    }

    public OperatorScan(R initialValue,
	    SerializableBiFunction<R, ? super T, R> accumulator) {
	this(() -> initialValue, accumulator);
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super R> t) {
	return new ScanSubscriber<>(initialValueFunction.get(), accumulator, t);
    }

    private static class ScanSubscriber<R, T> implements
	    SerializableSubscriber<T> {
	private static final long serialVersionUID = 1L;

	private final R initialValue;

	private SerializableBiFunction<R, ? super T, R> accumulator;

	private R value;

	private final Subscriber<? super R> child;

	boolean initialized = false;

	public ScanSubscriber(R initialValue,
		SerializableBiFunction<R, ? super T, R> accumulator,
		Subscriber<? super R> child) {
	    this.value = initialValue;
	    this.initialValue = initialValue;
	    this.child = child;
	    this.accumulator = accumulator;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void onNext(T currentValue) {
	    emitInitialValueIfNeeded(child);

	    if (NO_INITIAL_VALUE.equals(this.value)) {
		// if there is NO_INITIAL_VALUE then we know it is type T for
		// both so cast T to R
		this.value = (R) currentValue;
	    } else {
		try {
		    this.value = accumulator.apply(this.value, currentValue);
		} catch (Throwable e) {
		    onError(e);
		}
	    }
	    child.onNext(this.value);
	}

	@Override
	public void onComplete() {
	    emitInitialValueIfNeeded(child);
	    child.onComplete();
	}

	private void emitInitialValueIfNeeded(final Subscriber<? super R> child) {
	    if (!initialized) {
		initialized = true;

		if (!NO_INITIAL_VALUE.equals(initialValue)) {
		    child.onNext(initialValue);
		}
	    }
	}

	@Override
	public void onError(Throwable t) {
	    this.child.onError(t);

	}

	@Override
	public void onSubscribe(final Subscription s) {
	    this.accumulator = ManagedContextUtils
		    .tryInitialize(s, accumulator);
	    this.child.onSubscribe(new Subscription() {

		final AtomicBoolean once = new AtomicBoolean();

		final AtomicBoolean excessive = new AtomicBoolean();

		@Override
		public void request(long n) {
		    if (once.compareAndSet(false, true)) {

			if (NO_INITIAL_VALUE.equals(initialValue)
				|| n == Long.MAX_VALUE) {
			    s.request(n);
			} else if (n == 1) {
			    excessive.set(true);
			    s.request(1); // request at least 1
			} else {
			    // n != Long.MAX_VALUE && n != 1
			    s.request(n - 1);
			}
		    } else {
			// pass-thru after first time
			if (n > 1 // avoid to request 0
				&& excessive.compareAndSet(true, false)
				&& n != Long.MAX_VALUE) {
			    s.request(n - 1);
			} else {
			    s.request(n);
			}
		    }

		}

		@Override
		public void cancel() {
		    s.cancel();

		}
	    });

	}

    }

    private static class ImmutableObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((id == null) ? 0 : id.hashCode());
	    return result;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    ImmutableObject other = (ImmutableObject) obj;
	    if (id == null) {
		if (other.id != null)
		    return false;
	    } else if (!id.equals(other.id))
		return false;
	    return true;
	}
    }
}
