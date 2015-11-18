package it.reactive.muskel.server.hazelcast.listener;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MapEvent;

public abstract class AbstractEntryListener<K, V> implements
	EntryListener<K, V> {

    @Override
    public void entryAdded(EntryEvent<K, V> event) {

    }

    @Override
    public void entryUpdated(EntryEvent<K, V> event) {

    }

    @Override
    public void entryRemoved(EntryEvent<K, V> event) {

    }

    @Override
    public void entryEvicted(EntryEvent<K, V> event) {

    }

    @Override
    public void mapCleared(MapEvent event) {

    }

    @Override
    public void mapEvicted(MapEvent event) {

    }

}
