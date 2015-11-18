package it.reactive.muskel.internal.operators;

import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.functions.SerializableBiFunction;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.operator.utils.ComparatorUtils;
import it.reactive.muskel.internal.subscriber.AbstractSubscriber;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
public class OperatorToSortedList<T> implements Operator<List<T>, T> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public OperatorToSortedList() {
	this(ComparatorUtils.DEFAULT_SORT_FUNCTION);
    }

    @NonNull
    private final SerializableBiFunction<? super T, ? super T, Integer> sortFunction;

    @Override
    public Subscriber<? super T> apply(Subscriber<? super List<T>> t) {

	return new OperatorToSortedListSubScriber<>(t, sortFunction);
    }

    private static class OperatorToSortedListSubScriber<T> extends
	    AbstractSubscriber<T, List<T>> {

	private static final long serialVersionUID = 1L;
	private boolean completed = false;
	private final List<T> list = new LinkedList<T>();

	private SerializableBiFunction<? super T, ? super T, Integer> sortFunction;

	public OperatorToSortedListSubScriber(
		Subscriber<? super List<T>> t,
		SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	    super(t);
	    this.sortFunction = sortFunction;
	}

	@Override
	public void onSubscribe(Subscription s) {
	    this.sortFunction = ManagedContextUtils.tryInitialize(s,
		    sortFunction);
	    super.onSubscribe(s);
	}

	@Override
	public void request(long n) {
	    super.request(Long.MAX_VALUE);
	}

	@Override
	public void onComplete() {
	    try {
		completed = true;

		Collections.sort(list,
			ComparatorUtils.buildComparator(sortFunction));

		super.doOnNext(Collections.unmodifiableList(list));

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
