package it.reactive.muskel.internal.operators;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.exceptions.MuskelException;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.publisher.QueuePoolPublisher;
import it.reactive.muskel.internal.subscriber.QueueEmiterSubscriber;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@AllArgsConstructor
public class OperatorToLocal<T> implements Operator<T, T> {

    private static final long serialVersionUID = 1L;

    private final MuskelContext context;

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> o) {
	return new OperatorToLocalSubscriber<T>(context, o);
    }

    private static class OperatorToLocalSubscriber<T> extends
	    QueueEmiterSubscriber<T> {

	private static final long serialVersionUID = 1L;

	protected final String onSubscribeUUID;

	private final Subscriber<? super T> subscriber;

	// private final AtomicBoolean onSubscribeCalled = new AtomicBoolean();

	// Se false vuol dire che la classe non è stata inizializzata localmente
	private final transient Object localExecution;

	private OperatorToLocalSubscriber(MuskelContext context,
		Subscriber<? super T> subscriber) {
	    super(context, null);
	    this.onSubscribeUUID = context.generateIdentifier();
	    this.subscriber = subscriber;
	    final AtomicReference<String> registrationId = new AtomicReference<>();
	    localExecution = true;
	    registrationId.set(context.addMessageListener(onSubscribeUUID,
		    message -> {

			if (subcriptionUUID
				.equals(((SubscribeTopicMessage) message
					.getMessageObject()).getRequestId())) {

			    new QueuePoolPublisher<T>(subcriptionUUID, context,
				    null).subscribe(subscriber);

			    context.removeMessageListener(onSubscribeUUID,
				    registrationId.get());

			}
		    }));
	}

	@Override
	public void onSubscribe(final Subscription s) {
	    if (localExecution == null) {
		super.onSubscribe(s);
		context.publishMessage(onSubscribeUUID, SubscribeTopicMessage
			.builder().requestId(subcriptionUUID).build());
	    } else {
		throw new MuskelException(
			"The method onSubscribe should be invoked remotelly. Check use of Local Function");
	    }

	}

	@Override
	public void onError(Throwable t) {
	    if (localExecution == null) {
		super.onError(t);
	    } else {
		// Nel caso ci sia un errore prima dell'inizializzazione remota
		// cerco di proparare l'errore
		subscriber.onError(t);
	    }

	}

    }

    @Getter
    @Builder
    private static class SubscribeTopicMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String requestId;
    }

}