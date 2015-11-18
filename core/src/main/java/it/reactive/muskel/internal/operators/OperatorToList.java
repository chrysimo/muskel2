package it.reactive.muskel.internal.operators;

import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.reactivestreams.Subscriber;

public class OperatorToList<T> implements Operator<List<T>, T> {

    private static final long serialVersionUID = 1L;

    @Override
    public Subscriber<? super T> apply(final Subscriber<? super List<T>> t) {
	return new OperatorToListSubScriber<T>(t);
    }

    private static class OperatorToListSubScriber<T> extends
	    AbstractSubscriber<T, List<T>> {

	private static final long serialVersionUID = 1L;
	private boolean completed = false;
	private final List<T> list = new LinkedList<T>();

	public OperatorToListSubScriber(Subscriber<? super List<T>> t) {
	    super(t);
	}

	@Override
	public void request(long n) {
	    super.request(Long.MAX_VALUE);
	}

	@Override
	public void onComplete() {
	    try {
		completed = true;

		getChild().onNext(new ArrayList<T>(list));
		super.onComplete();
	    } catch (Throwable e) {
		onError(e);
	    }
	}

	@Override
	public void onNext(T value) {
	    if (!completed) {
		list.add(value);
	    }
	}

    }

}
