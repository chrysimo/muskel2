package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.functions.Operator;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
public class OperatorCast<T, R> implements Operator<R, T> {

    private static final long serialVersionUID = 1L;
    @NonNull
    private final Class<R> castClass;

    @Override
    public Subscriber<? super T> apply(final Subscriber<? super R> o) {
	return new OperatorCastSubscribe<T, R>(castClass, o);
    }

    @AllArgsConstructor
    private static class OperatorCastSubscribe<T, R> implements Subscriber<T>,
	    Serializable {
	private static final long serialVersionUID = 1L;
	@NonNull
	private final Class<R> castClass;

	private final Subscriber<? super R> o;

	@Override
	public void onError(Throwable e) {
	    o.onError(e);
	}

	@Override
	public void onSubscribe(Subscription s) {
	    o.onSubscribe(s);
	}

	@Override
	public void onNext(T t) {
	    try {
		o.onNext(castClass.cast(t));
	    } catch (Throwable e) {
		onError(e);
	    }
	}

	@Override
	public void onComplete() {
	    o.onComplete();

	}
    }
}
