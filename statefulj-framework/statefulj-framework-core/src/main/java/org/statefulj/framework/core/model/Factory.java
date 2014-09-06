package org.statefulj.framework.core.model;

public interface Factory<T, CT> {
	
	T create(Class<T> clazz, String event, CT context);

}
