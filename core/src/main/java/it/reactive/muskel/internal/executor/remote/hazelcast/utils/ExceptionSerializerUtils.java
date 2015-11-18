package it.reactive.muskel.internal.executor.remote.hazelcast.utils;

import java.util.ArrayList;
import java.util.Collection;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionSerializerUtils {

    /**
     * Serialize exception.
     *
     * @param source
     *            the source
     * @return the throwable
     */
    public static RemoteInvocationExeption serializeException(Throwable source) {
	return serializeException(source, null);
    }

    /**
     * Serialize exception.
     *
     * @param source
     *            the source
     * @param elements
     *            the elements
     * @return the throwable
     */
    public static RemoteInvocationExeption serializeException(Throwable source,
	    Collection<Throwable> elements) {

	if (elements == null) {
	    elements = new ArrayList<>();
	}
	RemoteInvocationExeption result = null;
	if (source != null && (!elements.contains(source))) {

	    elements.add(source);

	    result = new RemoteInvocationExeption(source.getClass().toString(),
		    source.getMessage(), serializeException(source.getCause(),
			    elements));
	    result.setStackTrace(source.getStackTrace());
	}
	return result;
    }

    static class RemoteInvocationExeption extends RuntimeException {

	/**
		 * 
		 */
	private static final long serialVersionUID = 1L;

	private String originalClassName;

	public RemoteInvocationExeption(String originalClassName,
		String message, Throwable e) {
	    super(message, e);
	    this.originalClassName = originalClassName;
	}

	/**
	 * @return the originalClassName
	 */
	public String getOriginalClassName() {
	    return originalClassName;
	}

	/**
	 * @param originalClassName
	 *            the originalClassName to set
	 */
	public void setOriginalClassName(String originalClassName) {
	    this.originalClassName = originalClassName;
	}

    }

}
