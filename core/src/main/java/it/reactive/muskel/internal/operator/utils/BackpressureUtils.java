package it.reactive.muskel.internal.operator.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import lombok.experimental.UtilityClass;

/**
 * Utility functions for use with backpressure. Copied from RxJava
 *
 */
@UtilityClass
public final class BackpressureUtils {

    /**
     * Adds {@code n} to {@code requested} field and returns the value prior to
     * addition once the addition is successful (uses CAS semantics). If
     * overflows then sets {@code requested} field to {@code Long.MAX_VALUE}.
     * 
     * @param requested
     *            atomic field updater for a request count
     * @param object
     *            contains the field updated by the updater
     * @param n
     *            the number of requests to add to the requested count
     * @return requested value just prior to successful addition
     */
    public static <T> long getAndAddRequest(
	    AtomicLongFieldUpdater<T> requested, T object, long n) {
	// add n to field but check for overflow
	while (true) {
	    long current = requested.get(object);
	    long next = addCap(current, n);
	    if (requested.compareAndSet(object, current, next)) {
		return current;
	    }
	}
    }

    /**
     * Adds {@code n} to {@code requested} and returns the value prior to
     * addition once the addition is successful (uses CAS semantics). If
     * overflows then sets {@code requested} field to {@code Long.MAX_VALUE}.
     * 
     * @param requested
     *            atomic long that should be updated
     * @param n
     *            the number of requests to add to the requested count
     * @return requested value just prior to successful addition
     */
    public static long getAndAddRequest(AtomicLong requested, long n) {
	// add n to field but check for overflow
	while (true) {
	    long current = requested.get();
	    long next = addCap(current, n);
	    if (requested.compareAndSet(current, next)) {
		return current;
	    }
	}
    }

    /**
     * Multiplies two positive longs and caps the result at Long.MAX_VALUE.
     * 
     * @param a
     *            the first value
     * @param b
     *            the second value
     * @return the capped product of a and b
     */
    public static long multiplyCap(long a, long b) {
	long u = a * b;
	if (((a | b) >>> 31) != 0) {
	    if (b != 0L && (u / b != a)) {
		u = Long.MAX_VALUE;
	    }
	}
	return u;
    }

    /**
     * Adds two positive longs and caps the result at Long.MAX_VALUE.
     * 
     * @param a
     *            the first value
     * @param b
     *            the second value
     * @return the capped sum of a and b
     */
    public static long addCap(long a, long b) {
	long u = a + b;
	if (u < 0L) {
	    u = Long.MAX_VALUE;
	}
	return u;
    }

}
