package it.reactive.muskel.context;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.context.hazelcast.HazelcastMuskelContext;
import it.reactive.muskel.context.local.LocalMuskelContext;
import it.reactive.muskel.executor.MuskelExecutorService;
import it.reactive.muskel.executor.NamedMuskelExecutorService;
import it.reactive.muskel.internal.executor.local.InThreadNamedMuskelExecutorService;
import it.reactive.muskel.internal.executor.local.LocalNamedMuskelExecutorService;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import com.google.common.collect.Lists;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.nio.ssl.SSLContextFactory;

/**
 * Il the abstraction of real implementation of local or remote functions
 *
 */
public interface MuskelContext {

    static String DEFAULT_CLIENT_NAME = "muskel";

    static String DEFAULT_CLIENT_PASSWORD = "password";

    /**
     * Return the builder for local or remote context
     * 
     * @return a new instance of builder
     */
    public static MuskelContextBuilder builder() {
	return new MuskelContextBuilder();
    }

    /**
     * Generate a random identifier unique for the environment
     * 
     * @return a unique identifier
     */
    String generateIdentifier();

    <T> MuskelQueue<T> getQueue(String name);

    void publishMessage(String name, Object value);

    <T> String addMessageListener(String name, MuskelMessageListener<T> listener);

    boolean removeMessageListener(String name, String id);

    MuskelExecutorService getMuskelExecutorService();

    void closeQueue(String name);

    void close();

    void closeMessageListener(String name);

    MuskelManagedContext getManagedContext();

    public static class MuskelContextBuilder {
	private MuskelContextBuilder() {
	}

	public MuskelContextLocalBuilder local() {
	    return new MuskelContextLocalBuilder();
	}

	public MuskelContextClientBuilder client() {
	    return new MuskelContextClientBuilder();
	}

	@java.lang.Override
	public String toString() {
	    return "MuskelContextBuilder";
	}

    }

    public static class AbstractMuskelContexBuilder<T> {
	protected int poolSize = Runtime.getRuntime().availableProcessors();

	protected final List<NamedMuskelExecutorService> executors = Lists
		.newArrayList();

	public T defaultPoolSize(int poolSize) {
	    if (poolSize <= 0) {
		throw new IllegalArgumentException("PoolSize must be positive");
	    }
	    this.poolSize = poolSize;
	    return doReturn();
	}

	public T addLocalExecutor(int poolSize, String... supportedNames) {
	    executors.add(new LocalNamedMuskelExecutorService(poolSize,
		    supportedNames));
	    return doReturn();
	}

	public T addLocalExecutor(Executor executor, String... supportedNames) {
	    executors.add(new LocalNamedMuskelExecutorService(executor,
		    supportedNames));
	    return doReturn();
	}

	protected boolean existExecutorService(MuskelExecutor name) {
	    return executors.stream().anyMatch(
		    current -> current.supports(name));
	}

	protected T addDefaultsIfNotExist() {

	    final MuskelExecutor local = MuskelExecutor.local();
	    if (!existExecutorService(local)) {
		executors.add(new LocalNamedMuskelExecutorService(poolSize,
			local.getName()));
	    }

	    if (!existExecutorService(local)) {
		executors.add(new InThreadNamedMuskelExecutorService(local
			.getName()));
	    }
	    return doReturn();
	}

	@SuppressWarnings("unchecked")
	protected T doReturn() {
	    return (T) this;
	}
    }

    public static class MuskelContextLocalBuilder extends
	    AbstractMuskelContexBuilder<MuskelContextLocalBuilder> {
	private int poolSize = Runtime.getRuntime().availableProcessors();

	private MuskelContextLocalBuilder() {
	}

	@java.lang.Override
	public String toString() {
	    return "MuskelContextLocalBuilder";
	}

	public LocalMuskelContext build() {
	    addDefaultsIfNotExist();
	    return new LocalMuskelContext(poolSize,
		    executors.toArray(new NamedMuskelExecutorService[] {}));
	}
    }

    public static class MuskelContextClientBuilder extends
	    AbstractMuskelContexBuilder<MuskelContextClientBuilder> {
	private final ClientConfig clientConfig = new ClientConfig();

	private MuskelContextClientBuilder() {
	    name(DEFAULT_CLIENT_NAME).password(DEFAULT_CLIENT_PASSWORD);

	}

	/**
	 * Adds given addresses to candidate address list that client will use
	 * to establish initial connection
	 *
	 * @param addresses
	 *            to be added to initial address list
	 * @return configured {@link MuskelContextClientBuilder} for chaining
	 */
	public MuskelContextClientBuilder addAddress(String... addresses) {
	    clientConfig.getNetworkConfig().addAddress(addresses);
	    return this;
	}

	/**
	 * Sets the group name of the group.
	 *
	 * @param name
	 *            the name to set for the group
	 * @return the updated {@link MuskelContextClientBuilder}.
	 * @throws IllegalArgumentException
	 *             if name is null.
	 */
	public MuskelContextClientBuilder name(String name) {
	    clientConfig.getGroupConfig().setName(name);
	    return this;
	}

	/**
	 * Sets the password for the group.
	 *
	 * @param password
	 *            the password to set for the group
	 * @return the updated {@link MuskelContextClientBuilder}.
	 * @throws IllegalArgumentException
	 *             if password is null.
	 */

	public MuskelContextClientBuilder password(String password) {
	    clientConfig.getGroupConfig().setPassword(password);
	    return this;
	}

	/**
	 * Sets the {@link SSLContext} implementation object.
	 *
	 * @param factoryImplementation
	 *            the factory {@link SSLContext} implementation object
	 * @return this SSLConfig instance
	 */
	public MuskelContextClientBuilder sslContext(final SSLContext sslContext) {

	    clientConfig
		    .getNetworkConfig()
		    .getSSLConfig()
		    .setFactoryImplementation(
			    sslContext == null ? null
				    : new SSLContextFactory() {

					@Override
					public void init(Properties properties)
						throws Exception {

					}

					@Override
					public SSLContext getSSLContext() {

					    return sslContext;
					}
				    });
	    clientConfig.getNetworkConfig().getSSLConfig()
		    .setEnabled(sslContext != null);
	    return this;
	}

	/**
	 * If true, client will route the key based operations to owner of the
	 * key at the best effort. Default value is true.
	 *
	 * @return the updated {@link MuskelContextClientBuilder}.
	 * @return configured {@link MuskelContextClientBuilder} for chaining
	 */
	public MuskelContextClientBuilder smartRouting(boolean smartRouting) {
	    clientConfig.getNetworkConfig().setSmartRouting(smartRouting);
	    return this;
	}

	public MuskelContext build() {
	    addDefaultsIfNotExist();

	    return new HazelcastMuskelContext(
		    HazelcastClient.newHazelcastClient(clientConfig),
		    executors.toArray(new NamedMuskelExecutorService[] {}))
		    .start();
	}

	@java.lang.Override
	public String toString() {
	    return "MuskelContextRemoteBuilder";
	}

    }

}
