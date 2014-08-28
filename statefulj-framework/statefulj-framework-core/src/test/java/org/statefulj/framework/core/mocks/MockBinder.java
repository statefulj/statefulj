package org.statefulj.framework.core.mocks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.ReferenceFactory;

public class MockBinder implements EndpointBinder {

	@Override
	public String getKey() {
		return "mock";
	}

	@Override
	public Class<?> bindEndpoints(Class<?> clazz,
			Map<String, Method> eventMapping, ReferenceFactory refFactory)
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {

		return MockProxy.class;
	}

}
