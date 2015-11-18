package it.reactive.muskel.internal.operators;

import it.reactive.muskel.GroupedMuskelProcessor;
import it.reactive.muskel.functions.SerializableBiFunction;
import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.subscriber.AbstractOperatorGroupBySubScriber;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.reactivestreams.Subscriber;

@RequiredArgsConstructor
public class OperatorGroupBy<T, K> implements
	Operator<GroupedMuskelProcessor<K, T>, T> {

    private static final long serialVersionUID = 1L;
    @NonNull
    private final SerializableFunction<? super T, K> keySelector;

    private final SerializableBiFunction<? super T, ? super T, Integer> sortFunction;

    @Override
    public Subscriber<? super T> apply(
	    Subscriber<? super GroupedMuskelProcessor<K, T>> t) {
	return new OperatorGroupBySubScriber<>(t, keySelector, sortFunction);
    }

    private static class OperatorGroupBySubScriber<T, K>
	    extends
	    AbstractOperatorGroupBySubScriber<T, K, GroupedMuskelProcessor<K, T>> {

	private static final long serialVersionUID = 1L;

	public OperatorGroupBySubScriber(
		Subscriber<? super GroupedMuskelProcessor<K, T>> t,
		SerializableFunction<? super T, K> keySelector,
		SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	    super(t, keySelector, sortFunction);
	}

	@Override
	protected void doOnNext(K k, List<T> list) {
	    super.doOnNext(GroupedMuskelProcessor.fromIterable(k, list));

	}

    }

}
