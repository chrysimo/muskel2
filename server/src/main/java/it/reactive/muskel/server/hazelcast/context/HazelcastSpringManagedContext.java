package it.reactive.muskel.server.hazelcast.context;

import it.reactive.muskel.context.MuskelInjectAware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.hazelcast.core.ManagedContext;
import com.hazelcast.executor.impl.RunnableAdapter;

public class HazelcastSpringManagedContext implements ManagedContext,
	ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;

    @SuppressWarnings("rawtypes")
    public Object initialize(Object obj) {
	Object resultObject = obj;
	if (obj != null) {
	    if (obj instanceof RunnableAdapter) {
		RunnableAdapter adapter = (RunnableAdapter) obj;
		Object runnable = adapter.getRunnable();
		runnable = initializeIfSpringAwareIsPresent(runnable);
		adapter.setRunnable((Runnable) runnable);
	    } else {
		resultObject = initializeIfSpringAwareIsPresent(obj);
	    }
	}
	return resultObject;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object initializeIfSpringAwareIsPresent(Object obj) {
	Class clazz = obj.getClass();
	MuskelInjectAware s = (MuskelInjectAware) clazz
		.getAnnotation(MuskelInjectAware.class);
	Object resultObject = obj;
	if (s != null) {
	    beanFactory.autowireBean(obj);
	    resultObject = beanFactory.initializeBean(obj, clazz.getName());
	}
	return resultObject;
    }

    public void setApplicationContext(
	    final ApplicationContext applicationContext) throws BeansException {
	this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }
}
