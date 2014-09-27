/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.statefulj.framework.tests.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

public class ReflectionUtils {

	@SuppressWarnings("unchecked")
	public static <T> T invoke(Object obj, String methodName, Class<T> returnType, Object... parms) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ArrayList<Class<?>> parmClasses = new ArrayList<Class<?>>();
		for (Object parm : parms) {
			if (HttpServletRequest.class.isAssignableFrom(parm.getClass())) {
				parmClasses.add(HttpServletRequest.class);
			} else {
				parmClasses.add(parm.getClass());
			}
		}
		Method method = obj.getClass().getDeclaredMethod(methodName, parmClasses.toArray(new Class<?>[]{}));
		return (T)method.invoke(obj, parms);
	}

	public static void invoke(Object obj, String methodName, Object... parms) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ArrayList<Class<?>> parmClasses = new ArrayList<Class<?>>();
		for (Object parm : parms) {
			if (HttpServletRequest.class.isAssignableFrom(parm.getClass())) {
				parmClasses.add(HttpServletRequest.class);
			} else {
				parmClasses.add(parm.getClass());
			}
		}
		Method method = null;
		for(Method m : obj.getClass().getDeclaredMethods()) {
			if (m.getName().equals(methodName) && m.getParameterTypes().length == parmClasses.size()) {
				method = m;
				int i = 0;
				for(Class<?> parmClass : m.getParameterTypes()) {
					if (!parmClass.isAssignableFrom(parmClasses.get(i))) {
						m = null;
						break;
					}
					i++;
				}
			}
			if (method != null) {
				break;
			}
		}
		if (method == null) {
			throw new RuntimeException("Couldn't find method " + methodName);
		}
		method.invoke(obj, parms);
	}
}
