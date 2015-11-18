package it.reactive.muskel.internal.executor.local;

import java.io.Serializable;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.executor.NamedMuskelExecutorService;

public abstract class AbstractMuskelNamedExcecutorService implements
	NamedMuskelExecutorService, Serializable {

    public static final String ALLSUPPORTED = "*";
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    protected final String[] supportedNames;

    public AbstractMuskelNamedExcecutorService(String... supportedNames) {
	this.supportedNames = supportedNames;
    }

    protected abstract boolean isLocal();

    @Override
    public boolean supports(MuskelExecutor key) {
	boolean result = false;
	if (key != null) {
	    for (String current : supportedNames) {
		if (key.isLocal() == isLocal()
			&& (current.equals(ALLSUPPORTED) || current
				.startsWith(key.getName()))) {
		    result = true;
		    break;
		}
	    }
	}
	return result;
    }
}
