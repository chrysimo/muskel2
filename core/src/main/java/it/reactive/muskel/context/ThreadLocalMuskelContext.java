package it.reactive.muskel.context;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThreadLocalMuskelContext {

    private static final ThreadLocal<MuskelContext> element = new ThreadLocal<>();

    public static void set(MuskelContext context) {
	element.set(context);
    }

    public static MuskelContext get() {
	return element.get();
    }
}
