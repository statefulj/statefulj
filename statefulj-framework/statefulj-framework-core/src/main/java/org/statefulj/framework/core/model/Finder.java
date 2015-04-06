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

/**
 * Finder is responsible to locating and retrieving the Stateful Entity.  There are two methods which 
 * can be invoked - one without an Id for the Stateful Entity and one with the Id of the Stateful Entity.
 * 
 * @author Andrew Hall
 *
 * @param <T> Type of the Stateful Entity
 * @param <CT> Type of the Request Context
 */
public interface Finder<T, CT> {

	/**
	 * This find method is invoked when no Id can be determined from the input from the EndpointBinder.
	 * 
	 * @param clazz The Class of the Stateful Event
	 * @param event The Event
	 * @param context The Request Context
	 * 
	 * @return The Stateful Entity
	 */
	T find(Class<T> clazz, String event, CT context);
	
	/**
	 * @param clazz
	 * @param id
	 * @param event
	 * @param context
	 * @return
	 */
	T find(Class<T> clazz, Object id, String event, CT context);
	
}
