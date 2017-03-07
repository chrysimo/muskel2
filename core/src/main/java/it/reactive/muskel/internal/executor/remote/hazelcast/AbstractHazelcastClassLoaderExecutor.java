package it.reactive.muskel.internal.executor.remote.hazelcast;

import it.reactive.muskel.context.MuskelManagedContext;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.context.hazelcast.HazelcastMuskelContext;
import it.reactive.muskel.context.utils.ManagedContextUtils;
import it.reactive.muskel.executor.NamedMuskelExecutorService;
import it.reactive.muskel.internal.classloader.repository.ClassLoaderRepository;
import it.reactive.muskel.internal.utils.SerializerUtils;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

@Slf4j
@RequiredArgsConstructor
@NoArgsConstructor
public abstract class AbstractHazelcastClassLoaderExecutor<T, K> implements DataSerializable, HazelcastInstanceAware {

	@NonNull
	private String clientUUID;
	@NonNull
	private K target;

	private byte[] callableByteArray;

	@Inject
	private transient ClassLoaderRepository classloaderRepository;

	@Inject
	private transient MuskelManagedContext managedContext;

	@Inject
	private transient Collection<NamedMuskelExecutorService> executorServices;

	private HazelcastInstance hazelcastInstance;

	@SuppressWarnings("unchecked")
	protected T doOperation() {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		ClassLoader targetClassLoader = classloaderRepository.getOrCreateClassLoaderByClientUUID(clientUUID);

		try {
			Thread.currentThread().setContextClassLoader(targetClassLoader);
			final HazelcastMuskelContext context = new HazelcastMuskelContext(hazelcastInstance, targetClassLoader,
					clientUUID, executorServices.toArray(new NamedMuskelExecutorService[] {}))
							.appendManagedContext(managedContext);

			return ManagedContextUtils.executeWithContext(context, null, () -> {
				this.target = (K) context.getManagedContext()
						.initialize(SerializerUtils.deserialize(targetClassLoader, callableByteArray));

				long initialTime = System.currentTimeMillis();
				try {
					T result = doOperation(target);
					return result;
				} finally {

					long endTime = System.currentTimeMillis();
					if (log.isTraceEnabled()) {
						log.trace("Callable exectution time was {}", (endTime - initialTime));
					}
				}
			});
		} finally {
			Thread.currentThread().setContextClassLoader(classloader);
		}
	}

	protected abstract T doOperation(K target);

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(clientUUID);
		out.writeByteArray(
				callableByteArray == null ? SerializerUtils.serializeToByteArray(target) : callableByteArray);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		this.clientUUID = in.readUTF();
		this.callableByteArray = in.readByteArray();
	}

	@Override
	public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}

	@Override
	public String toString() {
		return "[clientUUID=" + clientUUID + ", target=" + target + "]";
	}

}
