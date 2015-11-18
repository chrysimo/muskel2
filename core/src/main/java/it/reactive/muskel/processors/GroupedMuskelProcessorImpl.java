package it.reactive.muskel.processors;

import it.reactive.muskel.GroupedMuskelProcessor;
import lombok.Getter;

import org.reactivestreams.Publisher;

@Getter
public class GroupedMuskelProcessorImpl<K, T> extends MuskelProcessorImpl<T>
	implements GroupedMuskelProcessor<K, T> {

    private static final long serialVersionUID = 1L;
    private final K key;

    public GroupedMuskelProcessorImpl(K key, Publisher<T> publisher) {
	super(publisher);
	this.key = key;
    }

    @Override
    public String toString() {
	return "GroupedMuskelProcessor [key=" + key + "]";
    }
}
