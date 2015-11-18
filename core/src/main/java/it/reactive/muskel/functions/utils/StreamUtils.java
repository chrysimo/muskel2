package it.reactive.muskel.functions.utils;

import it.reactive.muskel.iterators.BlockingIterator;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StreamUtils {

    /**
     * 
     * @param it
     * @param parallel
     * @return
     */
    public static <T> Stream<T> makeStream(Iterator<T> it, boolean parallel) {
	Spliterator<T> s = Spliterators.spliteratorUnknownSize(it, 0);
	Stream<T> st = StreamSupport.stream(s, parallel);
	if (it instanceof BlockingIterator) {

	    BlockingIterator<T> bit = (BlockingIterator<T>) it;
	    st = st.onClose(bit::close);
	}
	return st;
    }
}
