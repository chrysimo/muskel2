package it.reactive.muskel.internal.operator.utils;

import it.reactive.muskel.functions.SerializableBiFunction;

import java.io.Serializable;
import java.util.Comparator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ComparatorUtils {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final SerializableBiFunction DEFAULT_SORT_FUNCTION = (c1, c2) -> ((Comparable<Object>) c1)
	    .compareTo(c2);

    public static <T> Comparator<? super T> buildComparator(
	    SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	return (Comparator<? super T> & Serializable) (o1, o2) -> sortFunction
		.apply((T) o1, (T) o2);
    }

}
