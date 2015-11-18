package it.reactive.muskel.internal.functions;

import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.operator.utils.SentinelUtils.ErrorSentinel;
import it.reactive.muskel.internal.publisher.QueuePoolPublisher;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class QueueReferenceToMuskelProcessorFunction<T> implements
	SerializableFunction<Object, MuskelProcessor<? extends T>> {

    private static final long serialVersionUID = 1L;
    private final transient MuskelContext context;

    @Override
    public MuskelProcessor<? extends T> apply(Object t) {
	final MuskelProcessor<? extends T> result;
	if (SentinelUtils.isComplete(t)) {
	    result = MuskelProcessor.empty();
	} else {
	    if (t instanceof ErrorSentinel) {
		result = MuskelProcessor.error(((ErrorSentinel) t).getE());

	    } else {
		result = MuskelProcessor.create(new QueuePoolPublisher<>(String
			.valueOf(t), context, null));
	    }
	}
	return result;
    }

}
