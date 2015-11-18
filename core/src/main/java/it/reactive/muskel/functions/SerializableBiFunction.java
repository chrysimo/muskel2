package it.reactive.muskel.functions;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * A serializable form of {@link BiFunction}
 * 
 * @see BiFunction
 */
public interface SerializableBiFunction<T, U, R> extends BiFunction<T, U, R>,
	Serializable {

}
