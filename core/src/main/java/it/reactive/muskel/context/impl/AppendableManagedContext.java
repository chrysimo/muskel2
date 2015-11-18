package it.reactive.muskel.context.impl;

import it.reactive.muskel.context.MuskelManagedContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppendableManagedContext implements MuskelManagedContext {

    private final List<MuskelManagedContext> managedContexts = new ArrayList<>();

    public AppendableManagedContext(MuskelManagedContext... managedContext) {
	append(managedContext);
    }

    @Override
    public Object initialize(Object obj) {
	if (managedContexts != null) {
	    for (MuskelManagedContext current : managedContexts) {
		if (current != null) {
		    obj = current.initialize(obj);
		}
	    }

	}
	return obj;
    }

    public void append(MuskelManagedContext... managedContext) {
	this.managedContexts.addAll(Arrays.asList(managedContext));
    }

}
