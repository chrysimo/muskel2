package it.reactive.muskel.functions;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * A serializable form of {@link Predicate}
 * 
 * @see Predicate
 */
public interface SerializablePredicate<T> extends Predicate<T>, Serializable {

}
