package it.reactive.muskel.internal.subscriber;

import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.functions.SerializableBiFunction;
import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.internal.operator.utils.ComparatorUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class AbstractOperatorGroupBySubScriber<T, K, Z> extends
	AbstractSubscriber<T, Z> {

    private static final long serialVersionUID = 1L;
    private boolean completed = false;
    private final List<T> list = new LinkedList<T>();

    private SerializableBiFunction<? super T, ? super T, Integer> sortFunction;

    private SerializableFunction<? super T, K> keySelector;

    public AbstractOperatorGroupBySubScriber(Subscriber<? super Z> t,
	    SerializableFunction<? super T, K> keySelector,
	    SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	super(t);
	this.keySelector = keySelector;
	this.sortFunction = sortFunction;
    }

    @Override
    public void onSubscribe(Subscription s) {
	this.sortFunction = ManagedContextUtils.tryInitialize(s, sortFunction);
	this.keySelector = ManagedContextUtils.tryInitialize(s, keySelector);
	super.onSubscribe(s);
    }

    @Override
    public void request(long n) {
	super.request(Long.MAX_VALUE);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onComplete() {
	if (!completed) {
	    try {
		completed = true;

		if (!list.isEmpty()) {

		    Map<K, List<T>> elements = list.stream().collect(
			    Collectors.groupingBy(keySelector));
		    if (sortFunction != null) {
			@SuppressWarnings("unchecked")
			Map<K, List<T>> treeMap = new TreeMap(
				ComparatorUtils.buildComparator(sortFunction));
			treeMap.putAll(elements);
			elements = treeMap;
		    }
		    elements.forEach((k, list) -> doOnNext(k, list));
		}
		super.onComplete();
	    } catch (Throwable e) {
		onError(e);
	    }
	}
    }

    protected abstract void doOnNext(K k, List<T> list);

    @Override
    public void onNext(T value) {
	if (!completed) {
	    list.add(value);
	}
    }

}