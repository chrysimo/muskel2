package it.reactive.muskel.server.hazelcast.context;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.collect.Lists;
import com.hazelcast.core.ManagedContext;

public class CompositeManagedContext implements ManagedContext,
	ApplicationContextAware {

    private final List<ManagedContext> contexts = Lists.newArrayList();

    public CompositeManagedContext(ManagedContext... contexts) {
	this.contexts.addAll(Arrays.asList(contexts));
    }

    @Override
    public Object initialize(Object obj) {

	for (ManagedContext current : contexts) {
	    obj = current.initialize(obj);
	}

	return obj;
    }

    @Override
    public String toString() {
	return "CompositeManagedContext [contexts=" + contexts + "]";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
	    throws BeansException {
	Optional.ofNullable(contexts)
		.ifPresent(
			value -> value
				.stream()
				.filter(current -> current instanceof ApplicationContextAware)
				.forEach(
					current -> ((ApplicationContextAware) current)
						.setApplicationContext(applicationContext)));

    }

}
