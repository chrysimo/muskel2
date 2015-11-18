package it.reactive.muskel.internal.operator.utils;

import java.io.Serializable;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@UtilityClass
public class SentinelUtils {

    public static class Sentinel implements Serializable {

	private static final long serialVersionUID = 1L;

    }

    public static class OnNullSentinel extends Sentinel {

	private static final long serialVersionUID = 1L;

	@Override
	public int hashCode() {
	    return 7;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    return true;
	}

	@Override
	public String toString() {
	    return "Notification=>NULL";
	}

    }

    /**
     * Wraps a Subscription.
     */
    public static final class SubscriptionNotification implements Serializable {
	/** */
	private static final long serialVersionUID = -1322257508628817540L;
	final Subscription s;

	SubscriptionNotification(Subscription s) {
	    this.s = s;
	}

	@Override
	public String toString() {
	    return "Notification.Subscription[" + s + "]";
	}
    }

    public static class OnCompleteSentinel extends Sentinel {

	private static final long serialVersionUID = 2L;

	@Override
	public int hashCode() {
	    return 31;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    return true;
	}

	@Override
	public String toString() {
	    return "Notification=>Completed";
	}

    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object n) {
	return n == ON_NEXT_NULL_SENTINEL ? null : (T) n;
    }

    public static <T> Object next(T t) {
	if (t == null)
	    return ON_NEXT_NULL_SENTINEL;
	else
	    return t;
    }

    public static OnCompleteSentinel complete() {
	return ON_COMPLETE_SENTINEL;
    }

    public static boolean isComplete(Object obj) {
	return ON_COMPLETE_SENTINEL.equals(obj);
    }

    /**
     * Converts a Subscription into a notification value.
     * 
     * @param e
     *            the Subscription to convert
     * @return the notification representing the Subscription
     */
    public static Object subscription(Subscription s) {
	return new SubscriptionNotification(s);
    }

    @AllArgsConstructor
    @Getter
    public static final class ErrorSentinel extends Sentinel {
	private static final long serialVersionUID = 23;

	public final Throwable e;

	@Override
	public int hashCode() {
	    return 47;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    return true;
	}

	@Override
	public String toString() {
	    return "Notification=>OnError";
	}

    }

    private static final OnCompleteSentinel ON_COMPLETE_SENTINEL = new OnCompleteSentinel();

    public static final OnNullSentinel ON_NEXT_NULL_SENTINEL = new OnNullSentinel();

    public static ErrorSentinel error(Throwable t) {
	return new ErrorSentinel(t);
    }

    public static boolean isError(Object obj) {
	return obj instanceof ErrorSentinel;
    }

    public static Throwable getError(Object obj) {
	if (isError(obj)) {
	    return ((ErrorSentinel) obj).getE();
	}
	return null;
    }

    public static <T> boolean emit(Subscriber<T> subscriber, Object v) {
	return emit(subscriber, v, null);
    }

    /**
     * Calls the appropriate Subscriber method based on the type of the
     * notification.
     * 
     * @param o
     *            the notification object
     * @param s
     *            the subscriber to call methods on
     * @return true if the notification was a terminal event (i.e., complete or
     *         error)
     * @see #accept(Object, Subscriber)
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean acceptFull(Object o, Subscriber<? super T> s) {
	if (ON_COMPLETE_SENTINEL.equals(o)) {
	    s.onComplete();
	    return true;
	} else if (o instanceof ErrorSentinel) {
	    s.onError(((ErrorSentinel) o).e);
	    return true;
	} else if (o instanceof SubscriptionNotification) {
	    s.onSubscribe(((SubscriptionNotification) o).s);
	    return false;
	}
	s.onNext((T) o);
	return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean emit(Subscriber<T> subscriber, Object v,
	    Consumer<Boolean> onCheck) {
	boolean result = false;
	if (v != null) {
	    if (v instanceof Sentinel) {
		if (v.equals(ON_NEXT_NULL_SENTINEL)) {
		    if (onCheck != null) {
			onCheck.accept(false);
		    }
		    subscriber.onNext(null);
		} else if (isComplete(v)) {
		    if (onCheck != null) {
			onCheck.accept(true);
		    }
		    subscriber.onComplete();
		    result = true;
		} else if (v instanceof ErrorSentinel) {
		    if (onCheck != null) {
			onCheck.accept(true);
		    }
		    subscriber.onError(((ErrorSentinel) v).e);
		    result = true;
		}
	    } else {
		if (onCheck != null) {
		    onCheck.accept(false);
		}
		subscriber.onNext((T) v);
	    }
	}
	return result;
    }
}
