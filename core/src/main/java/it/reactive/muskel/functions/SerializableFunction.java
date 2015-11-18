package it.reactive.muskel.functions;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * A serializable form of {@link Function}
 * 
 * @see Function
 */
public interface SerializableFunction<T, R> extends Function<T, R>,
	Serializable {

    default <V> SerializableFunction<T, V> andThen(
	    SerializableFunction<? super R, ? extends V> after) {
	Objects.requireNonNull(after);
	return (T t) -> after.apply(apply(t));
    }
}
