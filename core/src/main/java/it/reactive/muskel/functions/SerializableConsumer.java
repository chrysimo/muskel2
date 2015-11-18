package it.reactive.muskel.functions;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A serializable form of {@link Consumer}
 * 
 * @see Consumer
 */
public interface SerializableConsumer<T> extends Consumer<T>, Serializable {

}
