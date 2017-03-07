package it.reactive.muskel.internal.executor.local;

import it.reactive.muskel.context.MuskelContext;
import it.reactive.muskel.context.MuskelManagedContext;
import it.reactive.muskel.context.ThreadLocalMuskelContext;
import it.reactive.muskel.context.utils.ManagedContextUtils;
import lombok.NonNull;

public abstract class AbstractContextForward<T, K> {

	private final MuskelContext context;

	private final ClassLoader classloader = Thread.currentThread().getContextClassLoader();

	@NonNull
	private K target;

	public AbstractContextForward(K target) {
		this(target, ThreadLocalMuskelContext.get());
	}

	public AbstractContextForward(K target, MuskelContext context) {
		if (target == null) {
			throw new IllegalArgumentException("Target cannot be null");
		}
		this.context = context;
		this.target = target;
	}

	@SuppressWarnings("unchecked")
	protected T doOperation() {
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(classloader);
			return ManagedContextUtils.executeWithContext(context, () -> {
				if (context != null) {
					MuskelManagedContext managedContext = context.getManagedContext();
					target = (K) managedContext.initialize(target);
				}
				return doOperation(target);
			});
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}

	protected abstract T doOperation(K target);

}
