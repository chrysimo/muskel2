package it.reactive.muskel.context;

import java.util.concurrent.TimeUnit;

/**
 * This interface wraps the real implementation of queue
 *
 * @param <E>
 *            the type o queue
 */
public interface MuskelQueue<E> {

    /**
     * Retrieves and removes the head of this queue, waiting up to the specified
     * wait time if necessary for an element to become available.
     *
     * @param <E>
     *            The return type
     * @param timeout
     *            how long to wait before giving up, in units of {@code unit}
     * @param unit
     *            a {@code TimeUnit} determining how to interpret the
     *            {@code timeout} parameter
     * @return the head of this queue, or {@code null} if the specified waiting
     *         time elapses before an element is available
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.
     *
     * @param obj
     *            the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException
     *             if the class of the specified element prevents it from being
     *             added to this queue
     * @throws NullPointerException
     *             if the specified element is null
     * @throws IllegalArgumentException
     *             if some property of the specified element prevents it from
     *             being added to this queue
     */
    boolean offer(E obj);

    void clear();

}
