package it.reactive.muskel.internal.operators;

import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.functions.SerializablePredicate;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Filtra lo Stream in ingresso eliminando gli elementi che non rispettano il
 * predicato .
 * 
 */
@RequiredArgsConstructor
public class OperatorFilter<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;
    @NonNull
    private final SerializablePredicate<? super T> predicate;

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
	return new FilterSubscriber<T>(t, predicate);
    }

    private static class FilterSubscriber<T> extends AbstractSubscriber<T, T> {

	private static final long serialVersionUID = 1L;
	private SerializablePredicate<? super T> predicate;

	public FilterSubscriber(Subscriber<? super T> t,
		SerializablePredicate<? super T> predicate) {
	    super(t);
	    this.predicate = predicate;
	}

	@Override
	public void onSubscribe(Subscription s) {
	    this.predicate = ManagedContextUtils.tryInitialize(s, predicate);
	    super.onSubscribe(s);
	}

	@Override
	public void onNext(T t) {
	    try {
		if (predicate.test(t)) {
		    super.onNext(t);
		} else {
		    request(1);
		}
	    } catch (Throwable e) {
		super.onError(e);
	    }
	}
    }

}
