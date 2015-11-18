package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;
import lombok.AllArgsConstructor;

import org.reactivestreams.Subscriber;

@AllArgsConstructor
public class OperatorDefaultIfEmpty<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;
    private final T defaultValue;

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	return new DefaultIfEmptySubcriber<>(t, defaultValue);
    }

    private static class DefaultIfEmptySubcriber<T> extends
	    AbstractSubscriber<T, T> {

	private static final long serialVersionUID = 1L;

	private final T defaultValue;

	private boolean hasValue;

	public DefaultIfEmptySubcriber(Subscriber<? super T> t, T defaultValue) {
	    super(t);
	    this.defaultValue = defaultValue;
	}

	@Override
	public void onNext(T t) {
	    hasValue = true;
	    super.onNext(t);
	}

	@Override
	public void onComplete() {
	    if (!hasValue) {
		try {
		    super.onNext(defaultValue);
		} catch (Throwable e) {
		    super.onError(e);
		    return;
		}
	    }
	    super.onComplete();
	}

    }

}
