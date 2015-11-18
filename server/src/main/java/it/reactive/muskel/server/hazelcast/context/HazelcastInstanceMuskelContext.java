package it.reactive.muskel.server.hazelcast.context;

import it.reactive.muskel.context.MuskelManagedContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

@RequiredArgsConstructor
public class HazelcastInstanceMuskelContext implements MuskelManagedContext {

    @NonNull
    private final HazelcastInstance hazelcastInstance;

    @Override
    public Object initialize(Object obj) {
	if (obj instanceof HazelcastInstanceAware) {
	    ((HazelcastInstanceAware) obj)
		    .setHazelcastInstance(hazelcastInstance);
	}
	return obj;
    }

}
