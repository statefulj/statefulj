package org.statefulj.framework.core.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ReflectionUtils {

	@SuppressWarnings("unchecked")
	public static <T> T invoke(Object obj, String methodName, Class<T> returnType, Object... parms) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ArrayList<Class<?>> parmClasses = new ArrayList<Class<?>>();
		for (Object parm : parms) {
			parmClasses.add(parm.getClass());
		}
		Method method = obj.getClass().getDeclaredMethod(methodName, parmClasses.toArray(new Class<?>[]{}));
		return (T)method.invoke(obj, parms);
	}
}
