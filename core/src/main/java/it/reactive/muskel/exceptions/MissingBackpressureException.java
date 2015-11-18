package it.reactive.muskel.exceptions;

import it.reactive.muskel.MuskelProcessor;

/**
 * This Class is derived from RxJava Represents an exception that indicates that
 * a Subscriber or operator attempted to apply reactive pull backpressure to an
 * {@link MuskelProcessor} that does not implement it.
 * <p>
 * If an MuskelProcessor has not been written to support reactive pull
 * backpressure (such support is not a requirement for MuskelProcessors), you
 * can apply one of the following operators to it, each of which forces a simple
 * form of backpressure behavior:
 * <dl>
 * <dt><code>onBackpressureBuffer</code></dt>
 * <dd>maintains a buffer of all emissions from the source MuskelProcessor and
 * emits them to downstream Subscribers according to the requests they generate</dd>
 * <dt><code>onBackpressureDrop</code></dt>
 * <dd>drops emissions from the source MuskelProcessor unless there is a pending
 * request from a downstream Subscriber, in which case it will emit enough items
 * to fulfill the request</dd>
 * </dl>
 * If you do not apply either of these operators to an MuskelProcessor that does
 * not support backpressure, and if either you as the Subscriber or some
 * operator between you and the MuskelProcessor attempts to apply reactive pull
 * backpressure, you will encounter a {@code MissingBackpressureException} which
 * you will be notified of via your {@code onError} callback.
 * <p>
 * There are, however, other options. You can throttle an over-producing
 * MuskelProcessor with operators like {@code sample}/{@code throttleLast},
 * {@code throttleFirst}, or {@code throttleWithTimeout}/{@code debounce}. You
 * can also take the large number of items emitted by an over-producing
 * MuskelProcessor and package them into a smaller set of emissions by using
 * operators like {@code buffer} and {@code window}.
 */
public class MissingBackpressureException extends Exception {

    private static final long serialVersionUID = 7250870679677032194L;

    public MissingBackpressureException() {
    }

    public MissingBackpressureException(String message) {
	super(message);
    }

}
