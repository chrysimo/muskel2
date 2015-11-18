package it.reactive.muskel.internal.functions;

import java.io.Serializable;
import java.util.function.Function;

import org.reactivestreams.Subscriber;

public interface Operator<T, R> extends
	Function<Subscriber<? super T>, Subscriber<? super R>>, Serializable {

}
