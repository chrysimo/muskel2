package it.reactive.muskel.internal.executor.local;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class InThreadNamedMuskelExecutorService extends
	LocalNamedMuskelExecutorService {

    private static final long serialVersionUID = 1L;
    public static final Executor INSTANCE = new InThreadExecutor();

    public InThreadNamedMuskelExecutorService(String... supportedNames) {
	super(INSTANCE, supportedNames);
    }

    static class InThreadExecutor implements Executor {

	private InThreadExecutor() {
	}

	@Override
	public void execute(Runnable r) {
	    r.run();
	}
    }

    @Override
    public String toString() {
	return "InThreadNamedMuskelExecutorService ["
		+ Arrays.toString(supportedNames) + "]";
    }
}
