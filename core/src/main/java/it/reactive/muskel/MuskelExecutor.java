package it.reactive.muskel;

import it.reactive.muskel.utils.MuskelExecutorUtils;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Is the reference of Real Executor in local or remote machine.
 *
 */
public interface MuskelExecutor {

    /**
     * Return the default local Executor Reference
     * 
     * @return the default local Executor Reference
     */
    public static MuskelExecutor local() {
	return MuskelExecutorUtils.DEFAULT_LOCAL;
    }

    /**
     * Build a Local Executor Reference
     * 
     * @param name
     *            name of Executor
     * @return a Local Executor Reference
     */
    public static MuskelExecutor local(String name) {
	return new MuskelExecutorImpl(name, true);
    }

    /**
     * Return the default remote Executor Reference
     * 
     * @return the default remote Executor Reference
     */
    public static MuskelExecutor remote() {
	return MuskelExecutorUtils.DEFAULT_REMOTE;
    }

    /**
     * Build a remote Executor Reference
     * 
     * @param name
     *            name of Executor
     * @return a remote Executor Reference
     */
    public static MuskelExecutor remote(String name) {
	return new MuskelExecutorImpl(name, false);
    }

    /**
     * Return the name of executor
     * 
     * @return the name of executor
     */
    String getName();

    /**
     * Return is local or remote executor
     * 
     * @return true if the excecutor is local
     */
    boolean isLocal();

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    static class MuskelExecutorImpl implements MuskelExecutor, Serializable {

	private static final long serialVersionUID = 1L;

	@NonNull
	private final String name;

	private final boolean local;

	@Override
	public String toString() {
	    return "[" + name + "," + local + "]";
	}

    }
}
