package it.reactive.muskel.iterators;

import it.reactive.muskel.internal.operator.utils.SentinelUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;

import org.reactivestreams.Subscription;

public class BlockingIterator<T> implements Iterator<T>, AutoCloseable {
    final BlockingQueue<Object> queue;
    final Subscription resource;

    Object last;

    public BlockingIterator(BlockingQueue<Object> queue, Subscription resource) {
	this.queue = queue;
	this.resource = resource;
    }

    @Override
    public boolean hasNext() {
	if (last == null) {
	    Object o = queue.poll();
	    if (o == null) {
		try {
		    o = queue.take();
		} catch (InterruptedException ex) {
		    resource.cancel();
		    Thread.currentThread().interrupt();
		    throw new RuntimeException(ex);
		}
	    }
	    last = o;
	    if (SentinelUtils.isError(o)) {
		resource.cancel();
		Throwable e = SentinelUtils.getError(o);
		throw new RuntimeException(e);
	    }

	    if (SentinelUtils.isComplete(o)) {
		resource.cancel();
		return false;
	    }
	    return true;
	}
	Object o = last;
	if (SentinelUtils.isError(o)) {
	    Throwable e = SentinelUtils.getError(o);
	    throw new RuntimeException(e);
	}
	return !SentinelUtils.isComplete(o);
    }

    @Override
    public T next() {
	if (hasNext()) {
	    Object o = last;
	    last = null;
	    return SentinelUtils.getValue(o);
	}
	throw new NoSuchElementException();
    }

    @Override
    public void close() {
	resource.cancel();
    }

}
