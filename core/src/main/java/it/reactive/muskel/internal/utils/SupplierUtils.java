package it.reactive.muskel.internal.utils;

import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.functions.SerializableSupplier;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SupplierUtils {

    public static <T, R> SerializableFunction<T, R> getFunctionFromSupplier(
	    final SerializableSupplier<R> supplier) {
	return new SupplierToFunction<T, R>(supplier);
    }

    @AllArgsConstructor
    private static class SupplierToFunction<T, R> implements
	    SerializableFunction<T, R> {

	private static final long serialVersionUID = 1L;

	private final SerializableSupplier<R> supplier;

	@Override
	public R apply(Object t) {
	    return supplier.get();
	}
    }
}
