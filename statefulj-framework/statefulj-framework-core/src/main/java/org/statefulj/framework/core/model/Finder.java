package org.statefulj.framework.core.model;

public interface Finder<T, CT> {

	T find(Class<T> clazz, String event, CT context);
	
	T find(Class<T> clazz, Object id, String event, CT context);
	
}
