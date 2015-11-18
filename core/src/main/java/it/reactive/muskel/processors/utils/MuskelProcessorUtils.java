package it.reactive.muskel.processors.utils;

import lombok.experimental.UtilityClass;
import it.reactive.muskel.MuskelProcessor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@UtilityClass
public class MuskelProcessorUtils {

    /** Un muskelprocessor che emette solo onComplete dopo essere sottoscritto. */
    public static final MuskelProcessor<Object> EMPTY = MuskelProcessor
	    .create(new Publisher<Object>() {

		@Override
		public void subscribe(Subscriber<? super Object> s) {
		    s.onSubscribe(new Subscription() {

			@Override
			public void cancel() {

			}

			@Override
			public void request(long n) {
			    s.onComplete();

			}
		    });

		}
	    });
}
