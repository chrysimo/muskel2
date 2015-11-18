package it.reactive.muskel;

import it.reactive.muskel.internal.operator.utils.ComparatorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.testng.collections.Lists;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class QuickSort {

	private static final Comparator comparable = comparator();

	private static <T extends Comparable> Comparator<T> comparator() {
		return (first, second) -> {
			int result = first.compareTo(second);
			if (result < 0) {
				return -1;
			}
			if (result > 0) {
				return 1;
			}
			return 0;

		};
	}

	public static <K, T extends Comparable<K>> List<T> order(List<T> input) {
		return input != null && input.size() > 1 ? 
				order(MuskelProcessor.fromIterable(input),
				input.get(input.size() / 2)) : input;
	}

	public static <K, T extends Comparable<K>> List<T> order(
			MuskelProcessor<T> input, T element) {
		return input.groupBySortedToList(i -> comparable.compare(i, element))
				.map(t -> order(t.toBlocking().first()))
				.reduce(Lists.<T> newArrayList(), (t, r) -> {
					t.addAll(r);
					return t;
				}).toBlocking()
				.first();
	}

	@Test
	public void orderExample() {

		System.out.println(order(Arrays.asList(13, 3, 1, 6, 4)));
	}
}
