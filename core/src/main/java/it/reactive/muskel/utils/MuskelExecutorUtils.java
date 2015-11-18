package it.reactive.muskel.utils;

import lombok.experimental.UtilityClass;
import it.reactive.muskel.MuskelExecutor;

@UtilityClass
public class MuskelExecutorUtils {

    public static final String DEFAULT_LOCAL_NAME = "default";

    public static final String DEFAULT_REMOTE_NAME = "*";

    public static final String DEFAULT_LOCAL_THREAD_NAME = "thread";

    /**
     * Is the thread pool reference that execute the operation in default local
     * thread pool
     */
    public static final MuskelExecutor DEFAULT_LOCAL = MuskelExecutor
	    .local(DEFAULT_LOCAL_NAME);

    /**
     * Is the thread pool reference that execute the operation in random server
     * node
     */
    public static final MuskelExecutor DEFAULT_REMOTE = MuskelExecutor
	    .remote(DEFAULT_REMOTE_NAME);

    /**
     * Is the thread pool reference that execute in same thread of request
     */
    public static final MuskelExecutor DEFAULT_LOCAL_THREAD = MuskelExecutor
	    .local(DEFAULT_LOCAL_THREAD_NAME);
}
