package it.reactive.muskel.server.context;

import it.reactive.muskel.context.MuskelInjectAware;
import it.reactive.muskel.context.MuskelManagedContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class MuskelInjectAnnotationManagedContext implements
	MuskelManagedContext, ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;

    public MuskelInjectAnnotationManagedContext() {

    }

    public MuskelInjectAnnotationManagedContext(
	    ApplicationContext applicationContext) {
	setApplicationContext(applicationContext);
    }

    public Object initialize(Object obj) {
	Object resultObject = obj;
	if (obj != null) {
	    resultObject = initializeIfSpringAwareIsPresent(obj);
	}
	return resultObject;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object initializeIfSpringAwareIsPresent(Object obj) {
	Class clazz = obj.getClass();
	MuskelInjectAware s = (MuskelInjectAware) clazz
		.getAnnotation(MuskelInjectAware.class);
	if (s != null) {
	    beanFactory.autowireBean(obj);
	}
	return obj;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
	    throws BeansException {
	this.beanFactory = applicationContext.getAutowireCapableBeanFactory();

    }

}
