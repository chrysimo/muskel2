package it.reactive.muskel.internal.classloader.repository;

public interface ClassLoaderRepository {

    public ClassLoader getOrCreateClassLoaderByClientUUID(String clientUUID);
}
