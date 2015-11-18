package it.reactive.muskel.context.impl;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelContextAware;
import it.reactive.muskel.context.MuskelManagedContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ContextAwareManagedContext implements MuskelManagedContext {

    private final MuskelContext muskelContext;

    @Override
    public Object initialize(Object obj) {
	if (muskelContext != null && obj instanceof MuskelContextAware) {
	    ((MuskelContextAware) obj).setContext(muskelContext);
	}
	return obj;
    }

}
