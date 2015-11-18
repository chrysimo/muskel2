package it.reactive.muskel.exceptions;

public class ExecutorNotFoundException extends MuskelException {

    private static final long serialVersionUID = 1L;

    public ExecutorNotFoundException(String message) {
	super(message);
    }

    public ExecutorNotFoundException(Throwable t) {
	super(t);
    }

    public ExecutorNotFoundException(String message, Throwable t) {
	super(message, t);
    }
}
