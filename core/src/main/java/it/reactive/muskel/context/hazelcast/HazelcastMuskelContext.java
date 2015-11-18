package it.reactive.muskel.context.hazelcast;

import it.reactive.muskel.MuskelExecutor;
import it.reactive.muskel.context.Message;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelManagedContext;
import it.reactive.muskel.context.MuskelMessageListener;
import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.context.hazelcast.classloader.client.ClassloaderClientService;
import it.reactive.muskel.context.impl.AppendableManagedContext;
import it.reactive.muskel.context.impl.ContextAwareManagedContext;
import it.reactive.muskel.exceptions.ExecutorNotFoundException;
import it.reactive.muskel.executor.MuskelExecutorService;
import it.reactive.muskel.executor.NamedMuskelExecutorService;
import it.reactive.muskel.internal.executor.impl.MultipleMuskelExecutorService;
import it.reactive.muskel.internal.executor.local.AbstractMuskelNamedExcecutorService;
import it.reactive.muskel.internal.executor.remote.hazelcast.HazelcastClassLoaderCallable;
import it.reactive.muskel.internal.executor.remote.hazelcast.HazelcastClassLoaderRunnable;
import it.reactive.muskel.internal.utils.SerializerUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.core.MultiMap;

@Slf4j
public class HazelcastMuskelContext implements MuskelContext,
	HazelcastInstanceAware, Serializable {

    public static final String MEMBERSELECTOR_ATTRIBUTE_KEY = "customKey";

    private static final String NOT_REGEX = Pattern.quote("!");

    private static final String COMMA_SEPARATED_REGEX = Pattern.quote(";");

    private static final long serialVersionUID = 1L;

    private transient HazelcastInstance hazelcastInstance;

    private String clientUUID;

    private transient MultiMap<String, String> loadedObjects;

    private AppendableManagedContext managedContext = new AppendableManagedContext(
	    new ContextAwareManagedContext(this));

    private final transient MultipleMuskelExecutorService executorService = new MultipleMuskelExecutorService();

    private transient ClassLoader classLoader;

    public HazelcastMuskelContext(HazelcastInstance hazelcastInstance,
	    NamedMuskelExecutorService... additionalExecutorServices) {
	this(hazelcastInstance, null, null, additionalExecutorServices);

    }

    public HazelcastMuskelContext(HazelcastInstance hazelcastInstance,
	    ClassLoader classLoader, String clientUUID,
	    NamedMuskelExecutorService... additionalExecutorServices) {
	this.hazelcastInstance = hazelcastInstance;
	this.classLoader = classLoader;
	this.clientUUID = clientUUID;
	executorService.put(additionalExecutorServices);
	executorService.put(new HazelcastMuskelExecutorService(
		AbstractMuskelNamedExcecutorService.ALLSUPPORTED));
    }

    public HazelcastMuskelContext start() {
	this.clientUUID = getHazelcastInstance().getLocalEndpoint().getUuid();
	new ClassloaderClientService(this, clientUUID).start();
	return this;
    }

    @Override
    public String generateIdentifier() {
	IdGenerator idGenerator = getHazelcastInstance().getIdGenerator(
		clientUUID);
	return String.valueOf(idGenerator.newId());
    }

    @Override
    public MuskelManagedContext getManagedContext() {
	return this.managedContext;
    }

    public HazelcastMuskelContext appendManagedContext(
	    MuskelManagedContext... contexts) {
	this.managedContext.append(contexts);
	return this;
    }

    @Override
    public <T> MuskelQueue<T> getQueue(String name) {
	return getQueue(name, true);
    }

    public <T> MuskelQueue<T> getQueue(String name, boolean customSerialization) {
	return new HazelcastMuskelQueue<>(getHazelcastInstance().getQueue(
		registerQueue(name)), classLoader, customSerialization);
    }

    public <T, K> Map<T, K> getReplicatedMap(String name) {
	return getHazelcastInstance().getReplicatedMap(
		registerReplicatedMap(name));
    }

    @Override
    public void publishMessage(String name, Object value) {
	getHazelcastInstance().getTopic(registerQueue(name)).publish(value);

    }

    @Override
    public <T> String addMessageListener(String name,
	    MuskelMessageListener<T> listener) {
	return getHazelcastInstance().<T> getTopic(registerTopic(name))
		.addMessageListener(
			message -> listener.onMessage(new Message<T>(message
				.getMessageObject())));
    }

    @Override
    public boolean removeMessageListener(String name, String id) {

	return getHazelcastInstance().getTopic(name).removeMessageListener(id);
    }

    @Override
    public void closeQueue(String name) {
	getHazelcastInstance().getQueue(name).destroy();
	unregisterQueue(name);
    }

    @Override
    public void closeMessageListener(String name) {
	getHazelcastInstance().getTopic(name).destroy();
	unregisterTopic(name);
    }

    protected String registerReplicatedMap(String name) {
	getRepositoryMultiMap().put("replicatedmap", name);
	return name;
    }

    protected String registerQueue(String name) {
	getRepositoryMultiMap().put("queue", name);
	return name;
    }

    protected String unregisterQueue(String name) {
	getRepositoryMultiMap().remove("queue", name);
	return name;
    }

    protected String unregisterTopic(String name) {
	getRepositoryMultiMap().remove("topic", name);
	return name;
    }

    protected String registerTopic(String name) {
	getRepositoryMultiMap().put("topic", name);
	return name;
    }

    @Override
    public void close() {
	destroyTopics();
	destroyQueues();
	destroyReplicatedMaps();
	getRepositoryMultiMap().destroy();
	executorService.close();
    }

    public HazelcastMuskelContext addAllNamedMuskelExecutorService(
	    Collection<NamedMuskelExecutorService> executors) {
	if (executors != null) {
	    this.executorService.addAll(executors);
	}
	return this;
    }

    protected void destroyReplicatedMaps() {
	Collection<String> topics = getRepositoryMultiMap()
		.get("replicatedmap");
	if (topics != null) {
	    topics.stream()
		    .map(name -> getHazelcastInstance().getReplicatedMap(name))
		    .forEach(map -> map.destroy());
	}
    }

    protected void destroyTopics() {
	Collection<String> topics = getRepositoryMultiMap().get("topic");
	if (topics != null) {
	    topics.stream().map(name -> getHazelcastInstance().getTopic(name))
		    .forEach(topic -> topic.destroy());
	}
    }

    protected void destroyQueues() {
	Collection<String> queues = getRepositoryMultiMap().get("queue");
	if (queues != null) {
	    queues.stream().map(name -> getHazelcastInstance().getQueue(name))
		    .forEach(queue -> {
			try {
			    if (queue != null) {
				queue.destroy();
			    }
			} catch (Exception e) {
			    log.trace("Could not destroy queue " + queue, e);
			}
		    });
	}
    }

    protected MultiMap<String, String> getRepositoryMultiMap() {
	if (this.loadedObjects == null) {
	    this.loadedObjects = getHazelcastInstance().getMultiMap(
		    "repository_" + clientUUID);
	}
	return loadedObjects;
    }

    @Override
    public MuskelExecutorService getMuskelExecutorService() {
	return this.executorService;
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
	this.hazelcastInstance = hazelcastInstance;

    }

    public HazelcastInstance getHazelcastInstance() {
	if (hazelcastInstance == null) {
	    hazelcastInstance = Hazelcast
		    .getHazelcastInstanceByName("muskelServer");
	}
	return hazelcastInstance;
    }

    class HazelcastMuskelExecutorService extends
	    AbstractMuskelNamedExcecutorService implements Serializable {

	private static final long serialVersionUID = 1L;

	public HazelcastMuskelExecutorService(String... supportedNames) {
	    super(supportedNames);
	}

	@Override
	public <T> CompletableFuture<T> submitToKeyOwner(Supplier<T> task,
		MuskelExecutor key) {
	    IExecutorService executorService = getHazelcastInstance()
		    .getExecutorService(clientUUID);
	    final CompletableFuture<T> result = new CompletableFuture<>();

	    final MemberSelector memberSelector = getMemberSelector(key);
	    final HazelcastClassLoaderCallable<T> callable = new HazelcastClassLoaderCallable<>(
		    clientUUID, task);
	    final ExecutionCallback<T> executionCallback = new ExecutionCallback<T>() {

		@Override
		public void onResponse(T response) {
		    result.complete(response);
		}

		@Override
		public void onFailure(Throwable t) {
		    result.completeExceptionally(t);

		}
	    };

	    if (memberSelector == null) {
		executorService.submit(callable, executionCallback);
	    } else {
		try {
		    executorService.submit(callable, memberSelector,
			    executionCallback);
		} catch (RejectedExecutionException ree) {
		    throw new ExecutorNotFoundException(
			    "Could not find Remote Executor with " + key, ree);
		}
	    }

	    return result;
	}

	public void execute(Runnable runnable, MuskelExecutor key) {
	    IExecutorService executorService = getHazelcastInstance()
		    .getExecutorService(clientUUID);
	    final MemberSelector memberSelector = getMemberSelector(key);
	    final HazelcastClassLoaderRunnable targeRunnable = new HazelcastClassLoaderRunnable(
		    clientUUID, runnable);

	    if (memberSelector == null) {
		executorService.execute(targeRunnable);
	    } else {
		executorService.execute(targeRunnable, memberSelector);
	    }

	}

	protected MemberSelector getMemberSelector(MuskelExecutor key) {
	    return getMemberSelector(key.getName());
	}

	protected MemberSelector getMemberSelector(String key) {
	    return key == null || key.trim().equals("*") ? null
		    : new MemberSelector() {

			@Override
			public boolean select(Member member) {
			    boolean found = false;
			    String memberValue = member
				    .getStringAttribute(MEMBERSELECTOR_ATTRIBUTE_KEY);
			    if (memberValue != null) {
				String keyBase = key.startsWith(NOT_REGEX) ? key
					.replaceFirst(NOT_REGEX, "") : key;
				for (String current : memberValue
					.split(COMMA_SEPARATED_REGEX)) {

				    if (keyBase
					    .equalsIgnoreCase(current.trim())) {
					found = true;
					break;
				    }
				}
				if (key.startsWith(NOT_REGEX)) {
				    found = !found;
				}
			    }
			    return found;
			}

			public String toString() {
			    return "MemberSelector " + key;
			}
		    };
	}

	@Override
	public void close() {
	    getHazelcastInstance().getExecutorService(clientUUID).destroy();
	}

	@Override
	protected boolean isLocal() {
	    return false;
	}

    }

    @AllArgsConstructor
    static class HazelcastMuskelQueue<E> implements MuskelQueue<E> {

	@NonNull
	private final IQueue<Object> queue;

	private final ClassLoader classLoader;

	private final boolean serialize;

	@SuppressWarnings("unchecked")
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
	    Object result = this.queue.poll(timeout, unit);

	    return serialize ? SerializerUtils.deserialize(
		    classLoader == null ? Thread.currentThread()
			    .getContextClassLoader() : classLoader,
		    (byte[]) result) : (E) result;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((queue == null) ? 0 : queue.hashCode());
	    return result;
	}

	@Override
	public boolean offer(E obj) {
	    return this.queue.offer(serialize ? SerializerUtils
		    .serializeToByteArray(obj) : obj);
	}

	public void destroy() {
	    this.queue.destroy();

	}

	@Override
	public void clear() {
	    this.queue.clear();

	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    HazelcastMuskelQueue<?> other = (HazelcastMuskelQueue<?>) obj;
	    if (queue == null) {
		if (other.queue != null)
		    return false;
	    } else if (!queue.equals(other.queue))
		return false;
	    return true;
	}

    }

}
