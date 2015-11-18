package it.reactive.muskel.functions;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * A serializable base form of {@link Supplier}
 * 
 * @see Supplier
 */
public interface SerializableSupplier<T> extends Supplier<T>, Serializable {

}
