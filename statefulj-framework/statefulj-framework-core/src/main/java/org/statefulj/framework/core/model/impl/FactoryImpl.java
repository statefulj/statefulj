package org.statefulj.framework.core.model.impl;

import org.statefulj.framework.core.model.Factory;

public class FactoryImpl<T, CT> implements Factory<T, CT> {

	@Override
	public T create(Class<T> clazz, String event, CT context) {
		T newInstance = null;
		try {
			newInstance = clazz.newInstance();
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		return newInstance;
	}

}
