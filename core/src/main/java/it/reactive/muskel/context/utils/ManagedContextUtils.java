package it.reactive.muskel.context.utils;

import java.util.function.Supplier;

import org.reactivestreams.Subscription;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ManagedContextUtils {

	public static <T> T tryInitialize(Subscription s, T obj) {
		return tryInitialize(ThreadLocalMuskelContext.get(), obj);
	}

	@SuppressWarnings("unchecked")
	public static <T> T tryInitialize(MuskelContext context, T obj) {
		if (obj != null && context != null && context.getManagedContext() != null) {
			return (T) context.getManagedContext().initialize(obj);
		}
		return obj;
	}

	public static <T> T executeWithContext(MuskelContext context, Supplier<T> supplier) {
		return executeWithContext(context, ThreadLocalMuskelContext.get(), supplier);
	}

	public static <T> T executeWithContext(MuskelContext context, MuskelContext oldContext, Supplier<T> supplier) {
		try {

			ThreadLocalMuskelContext.set(context);
			return supplier.get();

		} finally {
			ThreadLocalMuskelContext.set(oldContext);
		}
	}
}
