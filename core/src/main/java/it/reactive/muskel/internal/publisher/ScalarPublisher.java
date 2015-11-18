package it.reactive.muskel.internal.publisher;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@AllArgsConstructor
@Builder
public class ScalarPublisher<T> implements Publisher<T> {

    private final T element;

    @Override
    public void subscribe(final Subscriber<? super T> s) {
	s.onSubscribe(new ScalarSubscription<>(s, element));
    }

    @AllArgsConstructor
    public static class ScalarSubscription<T> extends AtomicBoolean implements
	    Subscription {

	private static final long serialVersionUID = 1L;

	@NonNull
	private final Subscriber<? super T> s;

	private final T element;

	private final AtomicBoolean cancel = new AtomicBoolean();

	@Override
	public void request(long n) {

	    if (n < 0) {
		throw new IllegalArgumentException("n >= 0 required");
	    }
	    if (n == 0) {
		return;
	    }
	    // potrebbe arrivare una richiesta tra la onNext e la onComplete
	    if (!getAndSet(true)) {
		s.onNext(element);

		if (!cancel.get()) {
		    s.onComplete();
		}
	    }

	}

	@Override
	public void cancel() {
	    set(true);
	    cancel.set(true);

	}

    }

}
