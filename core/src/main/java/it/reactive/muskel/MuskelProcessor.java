package it.reactive.muskel;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.functions.SerializableBiFunction;
import it.reactive.muskel.functions.SerializableConsumer;
import it.reactive.muskel.functions.SerializableFunction;
import it.reactive.muskel.functions.SerializablePredicate;
import it.reactive.muskel.functions.SerializablePublisher;
import it.reactive.muskel.functions.SerializableSubscriber;
import it.reactive.muskel.functions.SerializableSubscriberBase;
import it.reactive.muskel.functions.SerializableSupplier;
import it.reactive.muskel.internal.functions.Operator;
import it.reactive.muskel.internal.functions.QueueBasedProcessorFunctionWrapper;
import it.reactive.muskel.internal.functions.QueueReferenceToMuskelProcessorFunction;
import it.reactive.muskel.internal.operator.utils.ComparatorUtils;
import it.reactive.muskel.internal.operators.OnSubscribeFromIterable;
import it.reactive.muskel.internal.operators.OnSubscribeRange;
import it.reactive.muskel.internal.operators.OperatorCast;
import it.reactive.muskel.internal.operators.OperatorConcat;
import it.reactive.muskel.internal.operators.OperatorDefaultIfEmpty;
import it.reactive.muskel.internal.operators.OperatorDoOnEach;
import it.reactive.muskel.internal.operators.OperatorExecuteOn;
import it.reactive.muskel.internal.operators.OperatorFilter;
import it.reactive.muskel.internal.operators.OperatorGroupBy;
import it.reactive.muskel.internal.operators.OperatorGroupByToList;
import it.reactive.muskel.internal.operators.OperatorMap;
import it.reactive.muskel.internal.operators.OperatorMerge;
import it.reactive.muskel.internal.operators.OperatorScan;
import it.reactive.muskel.internal.operators.OperatorSingle;
import it.reactive.muskel.internal.operators.OperatorSubscribeOn;
import it.reactive.muskel.internal.operators.OperatorTake;
import it.reactive.muskel.internal.operators.OperatorTakeLast;
import it.reactive.muskel.internal.operators.OperatorToList;
import it.reactive.muskel.internal.operators.OperatorToLocal;
import it.reactive.muskel.internal.operators.OperatorToSortedList;
import it.reactive.muskel.internal.operators.PublisherStreamSource;
import it.reactive.muskel.internal.subscriber.SubscriberUtils.OnCompleteSubscriber;
import it.reactive.muskel.internal.subscriber.SubscriberUtils.OnErrorSubscriber;
import it.reactive.muskel.internal.subscriber.SubscriberUtils.OnNextSubscriber;
import it.reactive.muskel.internal.utils.SupplierUtils;
import it.reactive.muskel.processors.MuskelProcessorImpl;
import it.reactive.muskel.processors.ScalarSynchronousMuskelProcessor;
import it.reactive.muskel.processors.ThrowMuskelProcessor;
import it.reactive.muskel.processors.utils.MuskelProcessorUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The MuskelProcessor interface that implements the Reactive Pattern.
 * <p>
 * This class provides methods for subscribing to the {@link Subscriber} as well
 * as delegate methods to the various {@link Publisher}.
 * 
 * This class uses funtions present in RxJava and {@link Stream}
 * 
 * @param <T>
 *            the type of the items emitted by the MuskelProcessor
 */
public interface MuskelProcessor<T> extends Serializable, AutoCloseable,
	SerializablePublisher<T> {

    /**
     * Returns a MuskelProcessor that emits the items emitted by each of the
     * MuskelProcessor emitted by the source Publisher, one after the other,
     * without interleaving them.
     * 
     * @param <T>
     *            the type of the items that this MuskelProcessor emits
     * @param processors
     *            an Publisher that emits MuskelProcessor
     * @return a MuskelProcessor that emits items all of the items emitted by
     *         the MuskelProcessor emitted by {@code processors}, one after the
     *         other, without interleaving them
     */
    public static <T> MuskelProcessor<T> concat(
	    MuskelProcessor<? extends MuskelProcessor<? extends T>> processors) {
	return processors.lift(OperatorConcat.<T> instance());
    }

    /**
     * Returns a MuskelProcessor that will execute the specified
     * {@link Publisher} when a {@link Subscriber} subscribes to it.
     * 
     * @param <T>
     *            the type of the items that this MuskelProcessor emits
     * @param publisher
     *            a function that accepts an {@code Publisher<T>}, and invokes
     *            its {@code onNext}, {@code onError}, and {@code onComplete}
     *            methods as appropriate
     * @return a MuskelProcessor that, when a {@link Subscriber} subscribes to
     *         it, will execute the specified function
     */
    public static <T> MuskelProcessor<T> create(Publisher<T> publisher) {
	return new MuskelProcessorImpl<T>(publisher);
    }

    /**
     * Returns a MuskelProcessor with specified {@link MuskelContext} that will
     * execute the specified {@link Publisher} when a {@link Subscriber}
     * subscribes to it.
     * 
     * @param <T>
     *            the type of the items that this MuskelProcessor emits
     * @param publisher
     *            a function that accepts an {@code Publisher<T>}, and invokes
     *            its {@code onNext}, {@code onError}, and {@code onComplete}
     *            methods as appropriate
     * @param context
     *            a {@link MuskelContext} that contains information about thread
     *            pools.
     * @return a MuskelProcessor that, when a {@link Subscriber} subscribes to
     *         it, will execute the specified function
     */
    public static <T> MuskelProcessor<T> create(Publisher<T> publisher,
	    MuskelContext context) {
	return new MuskelProcessorImpl<T>(publisher, context);
    }

    /**
     * Returns a MuskelProcessor that emits no items and immediately invokes its
     * {@link Subscriber#onComplete onComplete} method.
     * 
     * @param <T>
     *            the type of the items emitted by the MuskelProcessor
     * @return a MuskelProcessor that emits no items to the {@link Subscriber}
     *         but immediately invokes the {@link Subscriber}'s
     *         {@link Subscriber#onComplete() onComplete} method
     */
    @SuppressWarnings("unchecked")
    public static <T> MuskelProcessor<T> empty() {
	return (MuskelProcessor<T>) MuskelProcessorUtils.EMPTY;
    }

    /**
     * Returns a MuskelProcessor that invokes an {@link Subscriber}'s
     * {@link Subscriber#onError onError} method when the Subscriber subscribes
     * to it.
     * 
     * @param exception
     *            the particular Throwable to pass to {@link Subscriber#onError
     *            onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the
     *            MuskelProcessor
     * @return a MuskelProcessor that invokes the {@link Subscriber}'s
     *         {@link Subscriber#onError onError} method when the Observer
     *         subscribes to it
     */
    public static <T> MuskelProcessor<T> error(Throwable exception) {
	return new ThrowMuskelProcessor<T>(exception);
    }

    /**
     * Returns a MuskelProcessor that starts another {@link MuskelProcessor} in
     * a specific {@link MuskelExecutor}
     * 
     * 
     * @param supplier
     *            the function that supply a {@link MuskelProcessor}
     * @param executor
     *            the executor where exetute a {@link MuskelProcessor}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the
     *            MuskelProcessor
     * @return a MuskelProcessor that contains the results of supplied
     *         {@link MuskelProcessor}
     */
    public static <T> MuskelProcessor<T> executeOn(
	    SerializableSupplier<MuskelProcessor<T>> supplier,
	    MuskelExecutor executor) {
	return from(0).flatMap(SupplierUtils.getFunctionFromSupplier(supplier),
		executor);
    }

    /**
     * Converts an Array into a MuskelProcessor that emits the items in the
     * Array.
     * 
     * @param elements
     *            the source Array
     * @param <T>
     *            the type of items in the Array and the type of items to be
     *            emitted by the resulting MuskelProcessor
     * @return a MuskelProcessor that emits each item in the source Array
     */
    @SuppressWarnings("unchecked")
    public static <T> MuskelProcessor<T> from(T... elements) {
	return fromIterable(Arrays.asList(elements));
    }

    /**
     * Converts an {@link Iterable} sequence into a MuskelProcessor that emits
     * the items in the sequence.
     * 
     * @param elements
     *            the source {@link Iterable} sequence
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the
     *            type of items to be emitted by the resulting MuskelProcessor
     * @return a MuskelProcessor that emits each item in the source
     *         {@link Iterable} sequence
     */
    public static <T> MuskelProcessor<T> fromIterable(Iterable<T> elements) {
	return create(new OnSubscribeFromIterable<T>(elements));
    }

    /**
     * Converts an {@link SerializablePublisher} sequence into a MuskelProcessor
     * that emits the items in the sequence.
     * 
     * @param publisher
     *            the source {@link SerializablePublisher} sequence
     * @param <T>
     *            the type of items in the {@link SerializablePublisher}
     *            sequence and the type of items to be emitted by the resulting
     *            MuskelProcessor
     * @return a MuskelProcessor that emits each item in the source
     *         {@link SerializablePublisher} sequence
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> MuskelProcessor<T> fromPublisher(
	    SerializablePublisher<? extends T> publisher) {
	if (publisher instanceof MuskelProcessor) {
	    return (MuskelProcessor<T>) publisher;
	}
	return create(s -> publisher.subscribe((SerializableSubscriber) s));
    }

    /**
     * Converts an {@link Stream} sequence into a MuskelProcessor that emits the
     * items in the sequence.
     * 
     * @param stream
     *            the source {@link Stream} sequence
     * @param <T>
     *            the type of items in the {@link Stream} sequence and the type
     *            of items to be emitted by the resulting MuskelProcessor
     * @return a MuskelProcessor that emits each item in the source
     *         {@link Stream} sequence
     */
    public static <T> MuskelProcessor<T> fromStream(BaseStream<? extends T, ?> stream) {
	return create(new PublisherStreamSource<>(stream));
    }

    /**
     * Returns a MuskelProcessor that emits a single item and then completes.
     * 
     * @param value
     *            the item to emit
     * @param <T>
     *            the type of that item
     * @return a MuskelProcessor that emits {@code value} as a single item and
     *         then completes
     */
    static <T> MuskelProcessor<T> just(T value) {
	return ScalarSynchronousMuskelProcessor.create(value);
    }

    /**
     * Flattens a MuskelProcessor that emits {@link Publisher} into a single
     * MuskelProcessor that emits the items emitted by those MuskelProcessors,
     * without any transformation.
     *
     * @param <T>
     *            the type of the items that this MuskelProcessor emits
     * @param source
     *            a MuskelProcessor that emits MuskelProcessors
     * @return a MuskelProcessor that emits items that are the result of
     *         flattening the MuskelProcessors emitted by the {@code source}
     *         MuskelProcessor
     */
    public static <T> MuskelProcessor<T> merge(
	    MuskelProcessor<? extends MuskelProcessor<? extends T>> source) {
	return source.lift(OperatorMerge.<T> instance(false));
    }

    /**
     * Flattens an Array of MuskelProcessors into one MuskelProcessor, without
     * any transformation.
     * 
     * @param <T>
     *            the type of the items that this MuskelProcessor emits
     * @param sequences
     *            the Array of MuskelProcessors
     * @return a MuskelProcessor that emits items that are the result of
     *         flattening the items emitted by the MuskelProcessors in the Array
     */
    public static <T> MuskelProcessor<T> merge(
	    MuskelProcessor<? extends T>[] sequences) {
	return merge(from(sequences));
    }

    /**
     * Returns a MuskelProcessor that emits a sequence of Integers within a
     * specified range.
     * 
     * @param start
     *            the value of the first Integer in the sequence
     * @param count
     *            the number of sequential Integers to generate
     * @return a MuskelProcessor that emits a range of sequential Integers
     * @throws IllegalArgumentException
     *             if {@code count} is less than zero, or if {@code start} +
     *             {@code count} &minus; 1 exceeds {@code Integer.MAX_VALUE}
     */
    public static MuskelProcessor<Integer> range(int start, int count) {
	if (count < 0) {
	    throw new IllegalArgumentException("Count can not be negative");
	}
	if (count == 0) {
	    return empty();
	}
	if (start > Integer.MAX_VALUE - count + 1) {
	    throw new IllegalArgumentException(
		    "start + count can not exceed Integer.MAX_VALUE");
	}
	if (count == 1) {
	    return just(start);
	}
	return create(new OnSubscribeRange(start, start + (count - 1)));
    }

    /**
     * Returns a MuskelProcessor that emits the items emitted by the source
     * MuskelProcessor, converted to the specified type.
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param klass
     *            the target class type that {@code cast} will cast the items
     *            emitted by the source MuskelProcessor into before emitting
     *            them from the resulting MuskelProcessor
     * @return a MuskelProcessor that emits each item from the source
     *         MuskelProcessor after converting it to the specified type
     */
    default <R> MuskelProcessor<R> cast(final Class<R> klass) {
	return lift(new OperatorCast<T, R>(klass));
    }

    /**
     * Returns a new MuskelProcessor that emits items resulting from applying a
     * function that you supply to each item emitted by the source
     * MuskelProcessor, where that function returns a MuskelProcessor, and then
     * emitting the items that result from concatinating those resulting
     * MuskelProcessors.
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param func
     *            a function that, when applied to an item emitted by the source
     *            MuskelProcessor, returns a MuskelProcessor
     * @return a MuskelProcessor that emits the result of applying the
     *         transformation function to each item emitted by the source
     *         MuskelProcessor and concatinating the MuskelProcessors obtained
     *         from this transformation
     */
    default <R> MuskelProcessor<R> concatMap(
	    SerializableFunction<? super T, ? extends MuskelProcessor<? extends R>> func) {
	return concat(map(func));
    }

    /**
     * Returns a MuskelProcessor that emits the count of the total number of
     * items emitted by the source MuskelProcessor.
     * 
     * @return a MuskelProcessor that emits a single item: the number of
     *         elements emitted by the source MuskelProcessor
     */
    default MuskelProcessor<Long> count() {
	return reduce(0l, (Long t1, T t2) -> t1 + 1);

    }

    /**
     * Returns a MuskelProcessor that emits the items emitted by the source
     * MuskelProcessor or a specified default item if the source MuskelProcessor
     * is empty.
     * 
     * @param defaultValue
     *            the item to emit if the source MuskelProcessor emits no items
     * @return a MuskelProcessor that emits either the specified default item if
     *         the source MuskelProcessor emits no items, or the items emitted
     *         by the source MuskelProcessor
     */
    default MuskelProcessor<T> defaultIfEmpty(T defaultValue) {
	return lift(new OperatorDefaultIfEmpty<T>(defaultValue));
    }

    /**
     * Modifies the source MuskelProcessor so that it invokes an action when it
     * calls {@code onComplete}.
     * 
     * @param consumer
     *            the action to invoke when the source MuskelProcessor calls
     *            {@code onComplete}
     * @return the source MuskelProcessor with the side-effecting behavior
     *         applied
     */
    default MuskelProcessor<T> doOnComplete(SerializableConsumer<?> consumer) {
	return doOnEach(new OnCompleteSubscriber<T>(consumer));
    }

    /**
     * Modifies the source MuskelProcessor so that it invokes an action for each
     * item it emits.
     * 
     * @param subscriber
     *            the action to invoke for each item emitted by the source
     *            MuskelProcessor
     * @return the source MuskelProcessor with the side-effecting behavior
     *         applied
     */
    default MuskelProcessor<T> doOnEach(SerializableSubscriberBase<T> subscriber) {
	return lift(new OperatorDoOnEach<T>(subscriber));
    }

    /**
     * Modifies the source MuskelProcessor so that it invokes an action if it
     * calls {@code onError}.
     * 
     * @param consumer
     *            the action to invoke if the source MuskelProcessor calls
     *            {@code onError}
     * @return the source MuskelProcessor with the side-effecting behavior
     *         applied
     */
    default MuskelProcessor<T> doOnError(
	    SerializableConsumer<Throwable> consumer) {
	return doOnEach(new OnErrorSubscriber<T>(consumer));
    }

    /**
     * Modifies the source MuskelProcessor so that it invokes an action when it
     * calls {@code onNext}.
     * 
     * @param consumer
     *            the action to invoke when the source MuskelProcessor calls
     *            {@code onNext}
     * @return the source MuskelProcessor with the side-effecting behavior
     *         applied
     */
    default MuskelProcessor<T> doOnNext(SerializableConsumer<T> consumer) {
	return doOnEach(new OnNextSubscriber<T>(consumer));
    }

    /**
     * Modifies a MuskelProcessor to perform its emissions and notifications on
     * a specified {@link MuskelExecutor}, asynchronously.
     * 
     * @param executor
     *            the {@link MuskelExecutor} to notify {@link Subscriber}s on
     * @return the source MuskelProcessor modified so that its
     *         {@link Subscriber}s are notified on the specified
     *         {@link MuskelExecutor}
     */
    default MuskelProcessor<T> executeOn(MuskelExecutor executor) {

	return lift(new OperatorExecuteOn<T>(getContext(), executor));
    }

    /**
     * Filters items emitted by a MuskelProcessor by only emitting those that
     * satisfy a specified predicate.
     * 
     * @param predicate
     *            a {@link Predicate} that evaluates each item emitted by the
     *            source MuskelProcessor, returning {@code true} if it passes
     *            the filter
     * @return a MuskelProcessor that emits only those items emitted by the
     *         source MuskelProcessor that the filter evaluates as {@code true}
     */
    default MuskelProcessor<T> filter(SerializablePredicate<? super T> predicate) {
	return lift(new OperatorFilter<T>(predicate));
    }

    /**
     * Returns a MuskelProcessor that emits only the very first item emitted by
     * the source MuskelProcessor, or notifies of an
     * {@code NoSuchElementException} if the source MuskelProcessor is empty.
     * <p>
     * 
     * @return a MuskelProcessor that emits only the very first item emitted by
     *         the source MuskelProcessor, or raises an
     *         {@code NoSuchElementException} if the source MuskelProcessor is
     *         empty
     */
    default MuskelProcessor<T> first() {
	return take(1).single();
    }

    /**
     * Returns a MuskelProcessor that emits only the very first item emitted by
     * the source MuskelProcessor that satisfies a specified condition, or
     * notifies of an {@code NoSuchElementException} if no such items are
     * emitted.
     * 
     * @param predicate
     *            the condition that an item emitted by the source
     *            MuskelProcessor has to satisfy
     * @return a MuskelProcessor that emits only the very first item emitted by
     *         the source MuskelProcessor that satisfies the {@code predicate},
     *         or raises an {@code NoSuchElementException} if no such items are
     *         emitted
     */
    default MuskelProcessor<T> first(SerializablePredicate<? super T> predicate) {
	return takeFirst(predicate).single();
    }

    /**
     * Returns a MuskelProcessor that emits items computed by applying a user
     * supplied function on each item emitted by the source MuskelProcessor,
     * where that function returns a MuskelProcessor, and then merging those
     * resulting MuskelProcessors and emitting the results of this merger.
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param func
     *            a function that, when applied to an item emitted by the source
     *            MuskelProcessor, returns a MuskelProcessor
     * @return a MuskelProcessor that emits the result of applying the
     *         transformation function to each item emitted by the source
     *         MuskelProcessor and merging the results of the MuskelProcessors
     *         obtained from this transformation
     */
    default <R> MuskelProcessor<R> flatMap(
	    SerializableFunction<? super T, ? extends MuskelProcessor<? extends R>> func) {
	return merge(map(func));
    }

    /**
     * Returns a MuskelProcessor that emits items based on applying a function
     * that you supply to each item emitted by the source MuskelProcessor, where
     * that function returns a MuskelProcessor, and then merging those resulting
     * MuskelProcessors and emitting the results of this merger. Each
     * MuskelProcessor emitted by the funtion is subcribed in the executor
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param func
     *            a function that, when applied to an item emitted by the source
     *            MuskelProcessor, returns a MuskelProcessor
     * @param executor
     *            a executor that async subscribe each MuskelProcessor emitted
     *            by the function
     * @return a MuskelProcessor that emits the result of applying the
     *         transformation function to each item emitted by the source
     *         MuskelProcessor and merging the results of the MuskelProcessors
     *         obtained from this transformation
     */
    default <R> MuskelProcessor<R> flatMap(
	    SerializableFunction<? super T, ? extends MuskelProcessor<? extends R>> func,
	    MuskelExecutor executor) {

	final MuskelContext context = getContext();
	if (executor == null) {
	    throw new IllegalArgumentException("Executor cannot be null");
	}
	return executor.isLocal() ? merge(map(func
		.andThen(processor -> processor.withContext(context)
			.subscribeOn(executor)))) : merge(map(
		new QueueBasedProcessorFunctionWrapper<T, R>(func, context),
		executor).map(
		new QueueReferenceToMuskelProcessorFunction<R>(context)));
    }

    /**
     * Groups the items emitted by an {@code MuskelProcessor} according to a
     * specified criterion, and emits these grouped items as
     * {@link GroupedMuskelProcessor}s, one {@code MuskelProcessor} per group.
     * <em>Note:</em> A {@link GroupedMuskelProcessor} will cache the items it
     * is to emit until such time as it is subscribed to. For this reason, in
     * order to avoid memory leaks, you should not simply ignore those
     * {@code GroupedMuskelProcessor}s that do not concern you. Instead, you can
     * signal to them that they may discard their buffers by applying an
     * operator like {@link #take}{@code (0)} to them.
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @return an {@code MuskelProcessor} that emits
     *         {@link GroupedMuskelProcessor}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the
     *         source MuskelProcessor that share that key value
     */
    default <K> MuskelProcessor<GroupedMuskelProcessor<K, T>> groupBy(
	    final SerializableFunction<? super T, K> keySelector) {
	return groupBySorted(keySelector, null);
    }

    /**
     * Groups the items emitted by an {@code MuskelProcessor} according to a
     * specified criterion, and emits these grouped items as
     * {@link GroupedMuskelProcessor}s, one {@code MuskelProcessor} per group.
     * Each Group is ordered by natural order
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @throws ClassCastException
     *             if any item emitted by the MuskelProcessor does not implement
     *             {@link Comparable} with respect to all other items emitted by
     *             the MuskelProcessor
     * @param <K>
     *            the key type
     * @return an {@code MuskelProcessor} that emits
     *         {@link GroupedMuskelProcessor}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the
     *         source MuskelProcessor that share that key value
     */
    @SuppressWarnings("unchecked")
    default <K> MuskelProcessor<GroupedMuskelProcessor<K, T>> groupBySorted(
	    final SerializableFunction<? super T, K> keySelector) {
	return groupBySorted(keySelector, ComparatorUtils.DEFAULT_SORT_FUNCTION);
    }

    /**
     * Groups the items emitted by an {@code MuskelProcessor} according to a
     * specified criterion, and emits these grouped items as
     * {@link GroupedMuskelProcessor}s, one {@code MuskelProcessor} per group.
     * Each Group is ordered by sortFunction
     *
     * @throws ClassCastException
     *             if any item emitted by the MuskelProcessor does not implement
     *             {@link Comparable} with respect to all other items emitted by
     *             the MuskelProcessor
     * @param keySelector
     *            a function that extracts the key for each item
     * 
     * @param sortFunction
     *            a {@link SerializableBiFunction} that compares two items
     *            emitted by the source MuskelProcessor and returns an Integer
     *            that indicates their sort order
     * @param <K>
     *            the key type
     * @return an {@code MuskelProcessor} that emits
     *         {@link GroupedMuskelProcessor}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the
     *         source MuskelProcessor that share that key value
     */
    default <K> MuskelProcessor<GroupedMuskelProcessor<K, T>> groupBySorted(
	    final SerializableFunction<? super T, K> keySelector,
	    SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	return lift(new OperatorGroupBy<T, K>(keySelector, sortFunction));
    }

    /**
     * Groups the items emitted by an {@code MuskelProcessor} according to a
     * specified criterion, and emits these grouped items as
     * {@link GroupedMuskelProcessor}s, one {@code MuskelProcessor} per group
     * contains a list of groups. Each Group is ordered by natural order
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @throws ClassCastException
     *             if any item emitted by the MuskelProcessor does not implement
     *             {@link Comparable} with respect to all other items emitted by
     *             the MuskelProcessor
     * @param <K>
     *            the key type
     * @return an {@code MuskelProcessor} that emits
     *         {@link GroupedMuskelProcessor}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the
     *         source MuskelProcessor that share that key value
     */
    @SuppressWarnings("unchecked")
    default <K> MuskelProcessor<GroupedMuskelProcessor<K, List<T>>> groupBySortedToList(
	    final SerializableFunction<? super T, K> keySelector) {
	return groupBySortedToList(keySelector,
		ComparatorUtils.DEFAULT_SORT_FUNCTION);
    }

    /**
     * Groups the items emitted by an {@code MuskelProcessor} according to a
     * specified criterion, and emits these grouped items as
     * {@link GroupedMuskelProcessor}s, one {@code MuskelProcessor} per group
     * contains a list of groups. Each Group is ordered by natural order
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @param sortFunction
     *            a {@link SerializableBiFunction} that compares two items
     *            emitted by the source MuskelProcessor and returns an Integer
     *            that indicates their sort order
     * @throws ClassCastException
     *             if any item emitted by the MuskelProcessor does not implement
     *             {@link Comparable} with respect to all other items emitted by
     *             the MuskelProcessor
     * @param <K>
     *            the key type
     * @return an {@code MuskelProcessor} that emits
     *         {@link GroupedMuskelProcessor}s, each of which corresponds to a
     *         unique key value and each of which emits those items from the
     *         source MuskelProcessor that share that key value
     */
    default <K> MuskelProcessor<GroupedMuskelProcessor<K, List<T>>> groupBySortedToList(
	    final SerializableFunction<? super T, K> keySelector,
	    SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	return lift(new OperatorGroupByToList<T, K>(keySelector, sortFunction));
    }

    /**
     * Returns a MuskelProcessor that emits the last item emitted by the source
     * MuskelProcessor or notifies MuskelProcessors of a
     * {@code NoSuchElementException} if the source MuskelProcessor is empty.
     * 
     * @return a MuskelProcessor that emits the last item from the source
     *         MuskelProcessor or notifies MuskelProcessors of an error
     */
    default MuskelProcessor<T> last() {
	return takeLast(1).single();
    }

    /**
     * Lifts a function to the current MuskelProcessor and returns a new
     * MuskelProcessor that when subscribed to will pass the values of the
     * current MuskelProcessor through the Operator function.
     * <p>
     * In other words, this allows chaining Functions together on an
     * MuskelProcessor for acting on the values within the MuskelProcessor.
     * <p>
     * {@code
     * processor.map(...).filter(...).take(5).lift(new OperatorA()).lift(new OperatorB(...)).subscribe()
     * }
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param lift
     *            the Operator that implements the MuskelProcessor-operating
     *            function to be applied to the source MuskelProcessor
     * @return a MuskelProcessor that is the result of applying the lifted
     *         Operator to the source MuskelProcessor
     */
    <R> MuskelProcessor<R> lift(final Operator<? extends R, ? super T> lift);

    /**
     * Returns a MuskelProcessor that applies a specified function to each item
     * emitted by the source MuskelProcessor and emits the results of these
     * function applications.
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param function
     *            a function to apply to each item emitted by the
     *            MuskelProcessor
     * @return a MuskelProcessor that emits the items from the source
     *         MuskelProcessor, transformed by the specified function
     */
    default <R> MuskelProcessor<R> map(
	    SerializableFunction<? super T, ? extends R> function) {
	return map(function, null);
    }

    /**
     * Returns a MuskelProcessor that applies a specified function to each item
     * emitted by the source MuskelProcessor and emits the results of these
     * function applications. Each Function is called in another executor
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param function
     *            a function to apply to each item emitted by the
     *            MuskelProcessor
     * @param executor
     *            executor where execute a function
     * @return a MuskelProcessor that emits the items from the source
     *         MuskelProcessor, transformed by the specified function
     */
    default <R> MuskelProcessor<R> map(
	    SerializableFunction<? super T, ? extends R> function,
	    MuskelExecutor executor) {

	return lift(new OperatorMap<T, R>(function, getContext(), executor));
    }

    /**
     * Converts the source {@code MuskelProcessor<T>} into an
     * {@code MuskelProcessor<MuskelProcessor<T>>} that emits the source
     * MuskelProcessor as its single emission.
     * 
     * @return a MuskelProcessor that emits a single item: the source
     *         MuskelProcessor
     */
    default MuskelProcessor<MuskelProcessor<T>> nest() {
	return just(this);
    }

    /**
     * Returns a MuskelProcessor that applies a specified accumulator function
     * to the first item emitted by a source MuskelProcessor and a specified
     * seed value, then feeds the result of that function along with the second
     * item emitted by a MuskelProcessor into the same function, and so on until
     * all items have been emitted by the source MuskelProcessor, emitting the
     * final result from the final call to your function as its sole item. This
     * technique, which is called "reduce" here, is sometimec called
     * "aggregate," "fold," "accumulate," "compress," or "inject" in other
     * programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by
     *            the source MuskelProcessor, the result of which will be used
     *            in the next accumulator call
     * @return a MuskelProcessor that emits a single item that is the result of
     *         accumulating the output from the items emitted by the source
     *         MuskelProcessor
     * @see <a
     *      href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia:
     *      Fold (higher-order function)</a>
     */
    default <R> MuskelProcessor<R> reduce(R initialValue,
	    SerializableBiFunction<R, ? super T, R> accumulator) {
	return scan(initialValue, accumulator).last();
    }

    /**
     * Returns a MuskelProcessor that applies a specified accumulator function
     * to the first item emitted by a source MuskelProcessor, then feeds the
     * result of that function along with the second item emitted by the source
     * MuskelProcessor into the same function, and so on until all items have
     * been emitted by the source MuskelProcessor, and emits the final result
     * from the final call to your function as its sole item.
     * 
     * This technique, which is called "reduce" here, is sometimes called
     * "aggregate," "fold," "accumulate," "compress," or "inject" in other
     * programming contexts. Groovy, for instance, has an {@code inject} method
     * that does a similar operation on lists.
     * 
     * the type of the items that this MuskelProcessor emits
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by
     *            the source MuskelProcessor, whose result will be used in the
     *            next accumulator call
     * @return a MuskelProcessor that emits a single item that is the result of
     *         accumulating the items emitted by the source MuskelProcessor
     * @throws IllegalArgumentException
     *             if the source MuskelProcessor emits no items
     * @see <a
     *      href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia:
     *      Fold (higher-order function)</a>
     */
    default MuskelProcessor<T> reduce(
	    SerializableBiFunction<T, T, T> accumulator) {
	return scan(accumulator).last();
    }

    /**
     * Returns a MuskelProcessor that applies a specified accumulator function
     * to the first item emitted by a source MuskelProcessor and a seed value,
     * then feeds the result of that function along with the second item emitted
     * by the source MuskelProcessor into the same function, and so on until all
     * items have been emitted by the source MuskelProcessor, emitting the
     * result of each of these iterations. This sort of function is sometimes
     * called an accumulator.
     * <p>
     * Note that the MuskelProcessor that results from this method will emit
     * {@code initialValue} as its first emitted item.
     * 
     * @param <R>
     *            the type of the items that this MuskelProcessor emits
     * @param initialValue
     *            the initial (seed) accumulator item
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by
     *            the source MuskelProcessor, whose result will be emitted to
     *            {@link Subscriber}s via {@link Subscriber#onNext onNext} and
     *            used in the next accumulator call
     * @return a MuskelProcessor that emits {@code initialValue} followed by the
     *         results of each call to the accumulator function
     */
    default <R> MuskelProcessor<R> scan(R initialValue,
	    SerializableBiFunction<R, ? super T, R> accumulator) {
	return lift(new OperatorScan<R, T>(initialValue, accumulator));
    }

    /**
     * Returns a MuskelProcessor that applies a specified accumulator function
     * to the first item emitted by a source MuskelProcessor, then feeds the
     * result of that function along with the second item emitted by the source
     * MuskelProcessor into the same function, and so on until all items have
     * been emitted by the source MuskelProcessor, emitting the result of each
     * of these iterations.
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by
     *            the source MuskelProcessor, whose result will be emitted to
     *            {@link Subscriber}s via {@link Subscriber#onNext onNext} and
     *            used in the next accumulator call
     * @return a MuskelProcessor that emits the results of each call to the
     *         accumulator function
     */
    default MuskelProcessor<T> scan(SerializableBiFunction<T, T, T> accumulator) {
	return lift(new OperatorScan<T, T>(accumulator));
    }

    /**
     * Returns a MuskelProcessor that emits the single item emitted by the
     * source MuskelProcessor, if that MuskelProcessor emits only a single item.
     * If the source MuskelProcessor emits more than one item or no items,
     * notify of an {@code IllegalArgumentException} or
     * {@code NoSuchElementException} respectively.
     * 
     * @return a MuskelProcessor that emits the single item emitted by the
     *         source MuskelProcessor
     * @throws IllegalArgumentException
     *             if the source emits more than one item
     * @throws NoSuchElementException
     *             if the source emits no items
     */
    default MuskelProcessor<T> single() {
	return lift(new OperatorSingle<T>());
    }

    /**
     * Returns a MuskelProcessor that emits the single item emitted by the
     * source MuskelProcessor that matches a specified predicate, if that
     * MuskelProcessor emits one such item. If the source MuskelProcessor emits
     * more than one such item or no such items, notify of an
     * {@code IllegalArgumentException} or {@code NoSuchElementException}
     * respectively.
     * 
     * @param predicate
     *            a predicate function to evaluate items emitted by the source
     *            MuskelProcessor
     * @return a MuskelProcessor that emits the single item emitted by the
     *         source MuskelProcessor that matches the predicate
     * @throws IllegalArgumentException
     *             if the source MuskelProcessor emits more than one item that
     *             matches the predicate
     * @throws NoSuchElementException
     *             if the source MuskelProcessor emits no item that matches the
     *             predicate
     */
    default MuskelProcessor<T> single(SerializablePredicate<? super T> predicate) {
	return filter(predicate).single();
    }

    /**
     * Returns a MuskelProcessor that emits the single item emitted by the
     * source MuskelProcessor, if that MuskelProcessor emits only a single item,
     * or a default item if the source MuskelProcessor emits no items. If the
     * source MuskelProcessor emits more than one item, throw an
     * {@code IllegalArgumentException}.
     * 
     * @param defaultValue
     *            a default value to emit if the source MuskelProcessor emits no
     *            item
     * @return a MuskelProcessor that emits the single item emitted by the
     *         source MuskelProcessor, or a default item if the source
     *         MuskelProcessor is empty
     * @throws IllegalArgumentException
     *             if the source MuskelProcessor emits more than one item
     */
    default MuskelProcessor<T> singleOrDefault(T defaultValue) {
	return lift(new OperatorSingle<T>(defaultValue));
    }

    /**
     * Returns a MuskelProcessor that emits the single item emitted by the
     * source MuskelProcessor that matches a predicate, if that MuskelProcessor
     * emits only one such item, or a default item if the source MuskelProcessor
     * emits no such items. If the source MuskelProcessor emits more than one
     * such item, throw an {@code IllegalArgumentException}.
     * 
     * @param defaultValue
     *            a default item to emit if the source MuskelProcessor emits no
     *            matching items
     * @param predicate
     *            a predicate to evaluate items emitted by the source
     *            MuskelProcessor
     * @return a MuskelProcessor that emits the single item emitted by the
     *         source MuskelProcessor that matches the predicate, or the default
     *         item if no emitted item matches the predicate
     * @throws IllegalArgumentException
     *             if the source MuskelProcessor emits more than one item that
     *             matches the predicate
     */
    default MuskelProcessor<T> singleOrDefault(T defaultValue,
	    SerializablePredicate<? super T> predicate) {
	return filter(predicate).singleOrDefault(defaultValue);
    }

    /**
     * Subscribes to a MuskelProcessor and provides a callback to handle the
     * items it emits.
     * 
     * @param onNext
     *            the {@code SerializableConsumer<T>} you have designed to
     *            accept emissions from the MuskelProcessor
     * @return a {@link Subscription} reference with which the
     *         {@link Subscriber} can stop receiving items before the
     *         MuskelProcessor has finished sending them
     * @throws IllegalArgumentException
     *             if {@code onNext} is null
     */
    default void subscribe(final SerializableConsumer<? super T> onNext) {
	if (onNext == null) {
	    throw new IllegalArgumentException("onNext can not be null");
	}
	subscribe(new OnNextSubscriber<T>(onNext) {
	    private static final long serialVersionUID = 1L;

	    @Override
	    public final void onError(Throwable e) {
		if (e instanceof RuntimeException) {
		    throw (RuntimeException) e;
		} else {
		    throw new RuntimeException(e);
		}
	    }
	});
    }

    /**
     * Subscribes to a MuskelProcessor and provides a MuskelProcessor that
     * implements functions to handle the items the MuskelProcessor emits and
     * any error or completion notification it issues.
     *
     * @param subscriber
     *            the MuskelProcessor that will handle emissions and
     *            notifications from the MuskelProcessor
     * @return a {@link Subscription} reference with which the
     *         {@link Subscriber} can stop receiving items before the
     *         MuskelProcessor has completed
     */
    void subscribe(SerializableSubscriber<? super T> subscriber);

    /**
     * Asynchronously subscribes Publishers to this MuskelProcessor on the
     * specified {@link MuskelExecutor}.
     * 
     * @param executor
     *            the {@link MuskelExecutor} to perform subscription actions on
     * @return the source MuskelProcessor modified so that its subscriptions
     *         happen on the specified {@link MuskelExecutor}
     */
    default MuskelProcessor<T> subscribeOn(MuskelExecutor executor) {
	return nest().lift(new OperatorSubscribeOn<T>(getContext(), executor));
    }

    /**
     * Returns a MuskelProcessor that emits only the first {@code num} items
     * emitted by the source MuskelProcessor.
     * 
     * @param num
     *            the maximum number of items to emit
     * @return a MuskelProcessor that emits only the first {@code num} items
     *         emitted by the source MuskelProcessor, or all of the items from
     *         the source MuskelProcessor if that MuskelProcessor emits fewer
     *         than {@code num} items
     */
    default MuskelProcessor<T> take(final int num) {
	return take(num, false);
    }

    /**
     * Returns a MuskelProcessor that emits only the first {@code num} items
     * emitted by the source MuskelProcessor. If unbounded the source Subscriber
     * request max of possible items
     * 
     * @param num
     *            the maximum number of items to emit
     * @param unbounded
     *            If true the source Subscriber request max of possible items
     * @return a MuskelProcessor that emits only the first {@code num} items
     *         emitted by the source MuskelProcessor, or all of the items from
     *         the source MuskelProcessor if that MuskelProcessor emits fewer
     *         than {@code num} items
     */
    default MuskelProcessor<T> take(final int num, boolean unbounded) {
	return lift(new OperatorTake<T>(num, unbounded));
    }

    /**
     * Returns a MuskelProcessor that emits only the very first item emitted by
     * the source MuskelProcessor that satisfies a specified condition.
     * 
     * @param predicate
     *            the condition any item emitted by the source MuskelProcessor
     *            has to satisfy
     * @return a MuskelProcessor that emits only the very first item emitted by
     *         the source MuskelProcessor that satisfies the given condition, or
     *         that completes without emitting anything if the source
     *         MuskelProcessor completes without emitting a single
     *         condition-satisfying item
     */
    default MuskelProcessor<T> takeFirst(
	    SerializablePredicate<? super T> predicate) {
	return filter(predicate).take(1, true);
    }

    /**
     * Returns a MuskelProcessor that emits only the last {@code count} items
     * emitted by the source MuskelProcessor.
     * 
     * @param count
     *            the number of items to emit from the end of the sequence of
     *            items emitted by the source MuskelProcessor
     * @return a MuskelProcessor that emits only the last {@code count} items
     *         emitted by the source MuskelProcessor
     * @throws IndexOutOfBoundsException
     *             if {@code count} is less than zero
     */
    default MuskelProcessor<T> takeLast(final int count) {
	return lift(new OperatorTakeLast<T>(count));
    }

    /**
     * Converts a MuskelProcessor into a {@link BlockingMuskelProcessor} (an
     * MuskelProcessor with blocking operators).
     *
     * @return a {@code BlockingMuskelProcessor} version of this MuskelProcessor
     */
    default BlockingMuskelProcessor<T> toBlocking() {
	return BlockingMuskelProcessor.from(this);
    }

    /**
     * Returns a MuskelProcessor that emits a single item, a list composed of
     * all the items emitted by the source MuskelProcessor.
     * 
     * Normally, a MuskelProcessor that returns multiple items will do so by
     * invoking its {@link Subscriber}'s {@link Subscriber#onNext onNext} method
     * for each such item. You can change this behavior, instructing the
     * MuskelProcessor to compose a list of all of these items and then to
     * invoke the Subscriber's {@code onNext} function once, passing it the
     * entire list, by calling the Subscriber's {@code toList} method prior to
     * calling its {@link #subscribe} method.
     * <p>
     * Be careful not to use this operator on Subscribers that emit infinite or
     * very large numbers of items, as you do not have the option to
     * unsubscribe.
     * 
     * @return an Subscriber that emits a single item: a List containing all of
     *         the items emitted by the source Subscriber
     */
    default MuskelProcessor<List<T>> toList() {
	return lift(new OperatorToList<T>());
    }

    /**
     * Returns MuskelProcessor that emits a transfer a source MuskelProcessor
     * from remote node to local
     * 
     * @return MuskelProcessor that emits a transfer a source MuskelProcessor
     *         from remote node to local
     */
    default MuskelProcessor<T> toLocal() {

	return lift(new OperatorToLocal<T>(getContext()));
    }

    /**
     * Returns a MuskelProcessor that emits a list that contains the items
     * emitted by the source MuskelProcessor, in a sorted order. Each item
     * emitted by the MuskelProcessor must implement {@link Comparable} with
     * respect to all other items in the sequence.
     * 
     * @throws ClassCastException
     *             if any item emitted by the MuskelProcessor does not implement
     *             {@link Comparable} with respect to all other items emitted by
     *             the MuskelProcessor
     * @return a MuskelProcessor that emits a list that contains the items
     *         emitted by the source MuskelProcessor in sorted order
     */
    default MuskelProcessor<List<T>> toSortedList() {
	return lift(new OperatorToSortedList<>());
    }

    /**
     * Returns a MuskelProcessor that emits a list that contains the items
     * emitted by the source MuskelProcessor, in a sorted order based on a
     * specified comparison function.
     * 
     * @param sortFunction
     *            a {@link SerializableBiFunction} that compares two items
     *            emitted by the source MuskelProcessor and returns an Integer
     *            that indicates their sort order
     * @return a MuskelProcessor that emits a list that contains the items
     *         emitted by the source MuskelProcessor in sorted order
     */
    default MuskelProcessor<List<T>> toSortedList(
	    SerializableBiFunction<? super T, ? super T, Integer> sortFunction) {
	return lift(new OperatorToSortedList<T>(sortFunction));
    }

    /**
     * Assign the {@link MuskelContext} to the execution
     * 
     * @param context
     *            a {@link MuskelContext}
     * @return a MuskelProcessor with assigned {@link MuskelContext}
     */
    MuskelProcessor<T> withContext(MuskelContext context);

    /**
     * Returns the current {@link MuskelContext} assigned to execution
     * 
     * @return a {@link MuskelContext} assigned to {@link MuskelProcessor}
     */
    MuskelContext getContext();

    @Override
    void close();
}
