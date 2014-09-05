package org.statefulj.framework.core.model.impl;

import org.statefulj.framework.core.model.Factory;

public class FactoryImpl<T> implements Factory<T> {

	@Override
	public T create(Class<T> clazz) {
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
