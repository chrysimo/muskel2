package it.reactive.muskel.context.utils;

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
	if (obj != null && context != null
		&& context.getManagedContext() != null) {
	    return (T) context.getManagedContext().initialize(obj);
	}
	return obj;
    }

}
