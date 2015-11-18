package it.reactive.muskel.context.hazelcast.classloader.client;

import it.reactive.muskel.context.MuskelQueue;
import it.reactive.muskel.context.hazelcast.HazelcastMuskelContext;
import it.reactive.muskel.internal.Lifecycle;
import it.reactive.muskel.internal.classloader.domain.ResourceResponse;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;

public class ClassloaderClientService implements
	Lifecycle<ClassloaderClientService> {

    private final String clientUUID;

    private final ClassLoader classloader;

    private final HazelcastMuskelContext context;

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final AtomicBoolean started = new AtomicBoolean();

    public ClassloaderClientService(HazelcastMuskelContext context,
	    String clientUUID) {
	this(context, clientUUID, Thread.currentThread()
		.getContextClassLoader());
    }

    public ClassloaderClientService(HazelcastMuskelContext context,
	    String clientUUID, ClassLoader classloader) {
	this.context = context;
	this.clientUUID = clientUUID;
	this.classloader = classloader;
    }

    @Override
    public ClassloaderClientService start() {
	new Thread(new ResourceRequestRunnable(), "MuskelClassLoaderClient-"
		+ clientUUID).start();
	started.set(true);
	return this;
    }

    @Override
    public ClassloaderClientService stop() {
	stopped.set(true);

	if (isRunning()) {
	    MuskelQueue<String> queue = getRequestQueue();
	    Map<String, ResourceResponse> classContainer = getClassContainer();

	    queue.clear();
	    classContainer.clear();
	}
	return this;
    }

    protected MuskelQueue<String> getRequestQueue() {
	return context.getQueue("requestQueue_" + clientUUID, false);
    }

    protected Map<String, ResourceResponse> getClassContainer() {
	return context.getReplicatedMap("classCacheMap_" + clientUUID);
    }

    @Override
    public boolean isRunning() {
	return started.get();
    }

    public String doProcessSingle(MuskelQueue<String> queue) {
	String result = null;
	do {
	    try {
		result = queue.poll(1, TimeUnit.SECONDS);
	    } catch (InterruptedException e) {
	    }
	} while (result == null && !stopped.get());
	return result;
    }

    private ResourceResponse lookupClass(String req) {
	URL url = classloader.getResource(req);
	ResourceResponse res = new ResourceResponse(req);

	try {
	    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
		if (url != null) {
		    // byte[] classBytes = IOUtils.slurp(url);
		    byte[] classBytes = IOUtils.toByteArray(url);
		    res.setBytes(classBytes);
		}
	    }
	} catch (Exception e) {

	}
	return res;
    }

    private class ResourceRequestRunnable implements Runnable {

	@Override
	public void run() {
	    String request = null;
	    MuskelQueue<String> queue = getRequestQueue();
	    Map<String, ResourceResponse> classContainer = getClassContainer();
	    do {
		request = doProcessSingle(queue);
		if (request != null) {
		    classContainer.put(request, lookupClass(request));
		}
	    } while (request != null && !stopped.get());
	    started.set(false);
	}

    }

}
