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
 * 
 * Factory interface for creating an instance of the StatefulEntity.  The Factory is invoked by the
 * {@link org.statefulj.framework.core.model.FSMHarness} when the incoming request doesn't provide
 * an Id of the StatefulEntity or when the {@link org.statefulj.framework.core.model.Finder} was 
 * unable to locate the StatefulEntity.  
 * 
 * Implementations of the Factory are responsible for creating a new instance of the 
 * provided class based off the incoming Event and Request Context.
 * 
 * @author Andrew Hall
 *
 * @param <T> The Class of the StatefulEntity
 * @param <CT> The Request Context.  Exactly what the Request Context is dependent on the type of the EndpointBinder.
 */
public interface Factory<T, CT> {
	
	/**
	 * Called to create an instance of the StatefulEntity
	 * 
	 * @param clazz The type of the StatefulEntity
	 * @param event The incoming Event
	 * @param context The Request Context
	 * @return A new instance of the StatefulEntity
	 */
	T create(Class<T> clazz, String event, CT context);

}
