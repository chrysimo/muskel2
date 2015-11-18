package it.reactive.muskel.internal.functions;

import it.reactive.muskel.MuskelProcessor;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelContextAware;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.internal.operator.utils.SentinelUtils;
import it.reactive.muskel.internal.subscriber.QueueEmiterSubscriber;

import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
public class QueueBasedProcessorFunctionWrapper<T, R> implements
	SerializableFunction<T, Object>, MuskelContextAware {

    private static final long serialVersionUID = 1L;

    @NonNull
    private final SerializableFunction<? super T, ? extends MuskelProcessor<? extends R>> target;

    private transient MuskelContext context;

    @SuppressWarnings("unchecked")
    @Override
    public Object apply(T t) {
	try {
	    Function<? super T, ? extends MuskelProcessor<? extends R>> initializedTarget = target;
	    if (context != null && context.getManagedContext() != null) {
		initializedTarget = (Function<? super T, ? extends MuskelProcessor<? extends R>>) context
			.getManagedContext().initialize(this.target);
	    }
	    MuskelProcessor<? extends R> result = initializedTarget.apply(t);
	    MuskelContext currentContext = ThreadLocalMuskelContext.get();
	    if (currentContext != null) {
		result.withContext(currentContext);
	    }
	    QueueEmiterSubscriber<R> subscriber = new QueueEmiterSubscriber<>(
		    context, null);
	    result.subscribe(subscriber);
	    return subscriber.getSubcriptionUUID();
	} catch (Throwable e) {
	    return SentinelUtils.error(e);
	}

    }

    @Override
    public void setContext(MuskelContext context) {
	this.context = context;
    }

}
