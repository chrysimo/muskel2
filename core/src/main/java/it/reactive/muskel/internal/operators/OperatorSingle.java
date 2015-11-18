package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;

import java.util.NoSuchElementException;

import lombok.AllArgsConstructor;

import org.reactivestreams.Subscriber;

@AllArgsConstructor
public class OperatorSingle<T> implements Operator<T, T> {
    private static final long serialVersionUID = 1L;

    private final T defaultValue;

    public OperatorSingle() {
	this(null);
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	return new SingleSubscriver<>(t, defaultValue);
    }

    private static class SingleSubscriver<T> extends AbstractSubscriber<T, T> {

	private static final long serialVersionUID = 1L;

	private T value;
	private boolean isNonEmpty = false;
	private boolean hasTooManyElements = false;

	private final boolean hasDefaultValue;
	private final T defaultValue;

	public SingleSubscriver(Subscriber<? super T> t, T defaultValue) {
	    super(t);
	    this.defaultValue = defaultValue;
	    this.hasDefaultValue = defaultValue != null;
	}

	@Override
	public void onNext(T value) {
	    if (isNonEmpty) {
		hasTooManyElements = true;
		super.onError(new IllegalArgumentException(
			"Sequence contains too many elements"));
		unsubscribe();
	    } else {
		this.value = value;
		isNonEmpty = true;
		// Issue: https://github.com/ReactiveX/RxJava/pull/1527
		// Because we cache a value and don't emit now, we need to
		// request another one.
		request(1);
	    }
	}

	@Override
	public void onComplete() {
	    if (hasTooManyElements) {
		// We have already sent an onError message
	    } else {
		if (isNonEmpty) {
		    super.onNext(value);
		    super.onComplete();
		} else {
		    if (hasDefaultValue) {
			super.onNext(defaultValue);
			super.onComplete();
		    } else {
			super.onError(new NoSuchElementException(
				"Sequence contains no elements"));
		    }
		}
	    }
	}

    }

}
