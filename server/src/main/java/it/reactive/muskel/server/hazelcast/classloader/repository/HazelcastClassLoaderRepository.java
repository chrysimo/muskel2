package it.reactive.muskel.server.hazelcast.classloader.repository;

import it.reactive.muskel.context.hazelcast.HazelcastMuskelContext;
import it.reactive.muskel.internal.classloader.repository.ClassLoaderRepository;
import it.reactive.muskel.server.hazelcast.classloader.HazelcastClassLoader;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hazelcast.core.Client;
import com.hazelcast.core.ClientListener;
import com.hazelcast.core.HazelcastInstance;

@Slf4j
public class HazelcastClassLoaderRepository implements ClassLoaderRepository {

    private final HazelcastInstance hazelcastInstance;

    private final LoadingCache<String, HazelcastClassLoader> cacheStore = CacheBuilder
	    .newBuilder().maximumSize(1000)
	    .build(new CacheLoader<String, HazelcastClassLoader>() {
		public HazelcastClassLoader load(String key) {
		    return new HazelcastClassLoader(hazelcastInstance, key)
			    .start();
		}
	    });

    public HazelcastClassLoaderRepository(HazelcastInstance hazelcastInstance) {
	this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    public void init() {
	hazelcastInstance.getClientService().addClientListener(
		new ClientListener() {

		    @Override
		    public void clientDisconnected(Client client) {
			String uuid = client.getUuid();
			HazelcastClassLoader currentInstace = cacheStore
				.getIfPresent(uuid);
			if (currentInstace != null) {
			    new HazelcastMuskelContext(hazelcastInstance,
				    currentInstace, uuid).close();

			    cacheStore.invalidate(uuid);
			    log.trace(
				    "Client {} disconnected. Invalidated Classloader",
				    client);
			    currentInstace.stop();
			}
		    }

		    @Override
		    public void clientConnected(Client client) {
			log.trace("Client {} connected", client);

		    }
		});
    }

    @Override
    public HazelcastClassLoader getOrCreateClassLoaderByClientUUID(
	    String clientUUID) {
	return cacheStore.getUnchecked(clientUUID);
    }

}
