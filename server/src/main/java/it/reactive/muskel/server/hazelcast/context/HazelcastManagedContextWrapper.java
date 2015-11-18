package it.reactive.muskel.server.hazelcast.context;

import it.reactive.muskel.context.MuskelManagedContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.hazelcast.core.ManagedContext;

@RequiredArgsConstructor
public class HazelcastManagedContextWrapper implements MuskelManagedContext {

    @NonNull
    private final ManagedContext context;

    @Override
    public Object initialize(Object obj) {
	return context.initialize(obj);
    }

}
