package it.reactive.muskel.server.hazelcast.classloader;

import it.reactive.muskel.internal.Lifecycle;
import it.reactive.muskel.internal.classloader.domain.ResourceResponse;
import it.reactive.muskel.server.hazelcast.listener.AbstractEntryListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ReplicatedMap;

@Slf4j
public class HazelcastClassLoader extends ClassLoader implements
	Lifecycle<HazelcastClassLoader> {

    private ReplicatedMap<String, ResourceResponse> classCacheMap;

    private IQueue<String> requestQueue;

    private final Set<String> requestedResources = new TreeSet<>();

    private final String clientId;

    private final HazelcastInstance hazelcastInstance;

    private final AtomicBoolean running = new AtomicBoolean();

    private String entryListenerKey;

    private final Object lock = new Object();

    public HazelcastClassLoader(HazelcastInstance hazelcastInstance,
	    String clientId) {
	this(hazelcastInstance, clientId, Thread.currentThread()
		.getContextClassLoader());
    }

    public HazelcastClassLoader(HazelcastInstance hazelcastInstance,
	    String clientId, ClassLoader parent) {
	super(parent);
	this.clientId = clientId;
	this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public HazelcastClassLoader start() {
	log.trace("Starting Classloader for clientId {}", clientId);
	this.classCacheMap = hazelcastInstance
		.getReplicatedMap("classCacheMap_" + clientId);
	this.requestQueue = hazelcastInstance.getQueue("requestQueue_"
		+ clientId);

	this.entryListenerKey = this.classCacheMap
		.addEntryListener(new AbstractEntryListener<String, ResourceResponse>() {
		    @Override
		    public void entryUpdated(
			    EntryEvent<String, ResourceResponse> event) {
			doNotify();
		    }

		    @Override
		    public void entryAdded(
			    EntryEvent<String, ResourceResponse> event) {
			doNotify();
		    }

		    protected void doNotify() {
			synchronized (lock) {
			    lock.notifyAll();
			}
		    }
		});
	running.set(true);
	log.debug("Started Classloader for clientId {}", clientId);
	return this;
    }

    @Override
    public HazelcastClassLoader stop() {
	if (isRunning()) {
	    log.trace("Stopping Classloader for clientId {}", clientId);
	    if (this.classCacheMap != null) {
		if (this.entryListenerKey != null) {
		    this.classCacheMap
			    .removeEntryListener(this.entryListenerKey);
		}
		this.classCacheMap.clear();
	    }
	    if (this.requestQueue != null) {
		this.requestQueue.clear();
	    }
	    log.debug("Stopped Classloader for clientId {}", clientId);
	}
	return this;
    }

    @Override
    public boolean isRunning() {
	return running.get();
    }

    private ResourceResponse doGetResourceResponse(String name) {

	ResourceResponse resourceResponse = null;
	int count = 0;
	do {
	    try {
		resourceResponse = classCacheMap.get(name);
	    } catch (NullPointerException npe) {
	    }
	    if (resourceResponse == null) {

		if (requestedResources.contains(name) || requestQueue.add(name)) {
		    requestedResources.add(name);
		    try {
			synchronized (lock) {
			    lock.wait(5000);
			}

		    } catch (InterruptedException e) {
			return null;
		    }
		}
	    }
	    count++;
	} while (resourceResponse == null && count < 10);
	requestedResources.remove(name);
	return resourceResponse;

    }

    @Override
    protected URL findResource(String name) {

	ResourceResponse resourceResponse = doGetResourceResponse(name);
	try {
	    return resourceResponse == null ? null : new URL(name);
	} catch (MalformedURLException e) {
	    return null;
	}

    }

    /**
     * Returns an enumeration of URL objects representing all the resources with
     * th given name.
     *
     * Currently, WebSocketClassLoader returns only the first element.
     *
     * @param name
     *            The name of a resource.
     * @return All founded resources.
     */
    @Override
    protected Enumeration<URL> findResources(String name) {
	URL url = findResource(name);
	Vector<URL> urls = new Vector<>();
	if (url != null) {
	    urls.add(url);
	}
	return urls.elements();
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve)
	    throws ClassNotFoundException {
	synchronized (getClassLoadingLock(className)) {
	    Class<?> clazz = findLoadedClass(className);
	    if (clazz == null) {
		try {
		    clazz = getParent().loadClass(className);
		} catch (ClassNotFoundException ignored) {
		}
		if (clazz == null)
		    clazz = findClass(className);
	    }
	    if (resolve) {
		resolveClass(clazz);
	    }
	    return clazz;
	}
    }

    @Override
    protected Class<?> findClass(String className)
	    throws ClassNotFoundException {
	return defineClass(className);
    }

    private Class<?> defineClass(String className)
	    throws ClassNotFoundException {
	String path = className.replace('.', '/').concat(".class");
	ResourceResponse response = doGetResourceResponse(path);
	if (response == null || response.getBytes() == null
		|| response.getBytes().length <= 0)
	    throw new ClassNotFoundException(className);

	try {

	    byte[] bytes = response.getBytes();
	    if (bytes != null) {
		int idx = className.lastIndexOf(".");
		if (idx > 0) {
		    String packageName = className.substring(0, idx);
		    Package pkg = getPackage(packageName);
		    if (pkg == null) {
			definePackage(packageName, null, null, null, null,
				null, null, null);
		    }
		}
		return defineClass(className, bytes, 0, bytes.length);
	    } else {
		throw new ClassNotFoundException(className);
	    }
	} catch (Exception ex) {
	    throw new ClassNotFoundException(className, ex);
	}
    }

}
