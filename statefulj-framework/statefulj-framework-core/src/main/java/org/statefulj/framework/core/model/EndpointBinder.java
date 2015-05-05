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
package org.statefulj.framework.core.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.NotFoundException;

/**
 * An EndpointBinder is responsible for generating a Class which "binds" to an Endpoint Provider 
 * (SpringMVC, Jersey, Camel, etc..).  The EndpointBinders accept incoming input and forwards as an 
 * event to the {@link org.statefulj.framework.core.model.FSMHarness}
 * 
 * @author Andrew Hall
 *
 */
public interface EndpointBinder {
	
	/**
	 * Returns the "key" for this EndpointBinder.  The key is the first part of the tuple in an Event.
	 * For example, the key for the SpringMVC binder is "springmvc".  
	 * 
	 * @return The first part of the tuple in an Event
	 */
	String getKey();

	/**
	 * Invoked by the StatefulController to construct an EndpointBinder Class
	 * 
	 * @param beanName The Spring Bean Name for the Endpoint Binder
	 * @param stateControllerClass The class of the associated {@link org.statefulj.framework.core.annotations.StatefulController}
	 * @param idType The Class Type of the id field for the associated StatefulEntity
	 * @param isDomainEntity A flag indicating the StatefulEntity and the {@link org.statefulj.framework.core.annotations.StatefulController} are one in the same
	 * @param eventMapping Association of Event to the Action Method
	 * @param refFactory The {@link org.statefulj.framework.core.model.ReferenceFactory} that generates all Spring Bean ids
	 * @return The generated Class of the EndpointBinder 
	 * @throws CannotCompileException 
	 * @throws NotFoundException 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	Class<?> bindEndpoints(
			String beanName, 
			Class<?> stateControllerClass,
			Class<?> idType,
			boolean isDomainEntity,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException;
}
