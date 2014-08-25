package org.statefulj.framework.core.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.NotFoundException;

public interface EndpointBinder {
	
	String getKey();

	Class<?> bindEndpoints(
			Class<?> clazz, 
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException;
}
