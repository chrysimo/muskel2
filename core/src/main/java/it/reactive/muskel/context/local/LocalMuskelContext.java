package it.reactive.muskel.context.local;

import it.reactive.muskel.context.Message;
import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelManagedContext;
import it.reactive.muskel.context.MuskelMessageListener;
import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.context.impl.ContextAwareManagedContext;
import it.reactive.muskel.executor.MuskelExecutorService;
import it.reactive.muskel.executor.NamedMuskelExecutorService;
import it.reactive.muskel.internal.executor.impl.MultipleMuskelExecutorService;
import it.reactive.muskel.internal.utils.RingBuffer;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class LocalMuskelContext implements MuskelContext, Serializable,
	AutoCloseable {

    private static final long serialVersionUID = 1L;

    private final transient MultipleMuskelExecutorService executorService;

    private final AtomicLong identifier = new AtomicLong();

    private transient Multimap<String, MuskelMessageListenerContainer> messageListeners;

    private final MuskelManagedContext context = new ContextAwareManagedContext(
	    this);

    private final LoadingCache<String, MuskelQueue> queues = CacheBuilder
	    .newBuilder().build(new CacheLoader<String, MuskelQueue>() {

		public MuskelQueue load(String key) {
		    return new LocalMuskelQueue();
		}
	    });

    public LocalMuskelContext(
	    NamedMuskelExecutorService... otherExecutorServices) {
	this(Runtime.getRuntime().availableProcessors(), otherExecutorServices);
    }

    public LocalMuskelContext(int pooSize,
	    NamedMuskelExecutorService... otherExecutorServices) {
	executorService = new MultipleMuskelExecutorService();
	executorService.put(otherExecutorServices);

    }

    @Override
    public String generateIdentifier() {
	return String.valueOf(identifier.incrementAndGet());
    }

    @Override
    public <T> MuskelQueue<T> getQueue(String name) {
	return queues.getUnchecked(name);
    }

    @Override
    public void publishMessage(String name, Object value) {
	if (messageListeners != null) {
	    Message message = new Message(value);
	    Optional.ofNullable(messageListeners.get(name)).ifPresent(
		    present -> present.stream().map(curr -> curr.getListener())
			    .forEach(current -> current.onMessage(message)));
	}
    }

    @Override
    public <T> String addMessageListener(String name,
	    MuskelMessageListener<T> listener) {
	if (messageListeners == null) {
	    messageListeners = TreeMultimap.create();
	}
	MuskelMessageListenerContainer elementToAdd = new MuskelMessageListenerContainer(
		listener);
	messageListeners.put(name, elementToAdd);
	return elementToAdd.getKey();
    }

    @Override
    public boolean removeMessageListener(String name, String id) {
	return messageListeners != null ? messageListeners.remove(name, id)
		: false;
    }

    @Override
    public MuskelExecutorService getMuskelExecutorService() {
	return executorService;
    }

    @Override
    public void close() {
	if (messageListeners != null) {
	    messageListeners.clear();
	}
	if (executorService != null) {
	    executorService.close();
	}
	queues.invalidateAll();
    }

    @Override
    public MuskelManagedContext getManagedContext() {
	return context;
    }

    @Override
    public void closeQueue(String name) {
	queues.invalidate(name);

    }

    @Override
    public void closeMessageListener(String name) {
	messageListeners.removeAll(name);

    }

    private static class LocalMuskelQueue<T> implements Serializable,
	    MuskelQueue<T> {

	private static final long serialVersionUID = 1L;
	private final BlockingQueue<T> queue = new ArrayBlockingQueue<T>(
		RingBuffer.SIZE);

	@Override
	public T poll(long timeout, TimeUnit unit) throws InterruptedException {
	    return queue.poll(timeout, unit);
	}

	@Override
	public boolean offer(T obj) {
	    return queue.offer(obj);
	}

	@Override
	public void clear() {
	    queue.clear();

	}
    }

    @RequiredArgsConstructor
    @Getter
    private static class MuskelMessageListenerContainer implements
	    Comparable<MuskelMessageListenerContainer> {

	@NonNull
	private final MuskelMessageListener<?> listener;

	private final String key = UUID.randomUUID().toString();

	@Override
	public int compareTo(MuskelMessageListenerContainer o) {
	    return key.compareTo(o.key);
	}

	@Override
	public int hashCode() {
	    return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (obj instanceof String) {
		return key.equals((String) obj);
	    }
	    if (getClass() != obj.getClass())
		return false;
	    MuskelMessageListenerContainer other = (MuskelMessageListenerContainer) obj;
	    if (key == null) {
		if (other.key != null)
		    return false;
	    } else if (!key.equals(other.key))
		return false;
	    return true;
	}

    }
}
