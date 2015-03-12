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
package org.statefulj.framework.core.fsm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.fsm.RetryObserver;

/**
 * @author Andrew Hall
 *
 */
public class RetryObserverImpl<T, CT> implements RetryObserver<T> {

	private static final Logger logger = LoggerFactory.getLogger(FSM.class);

	private Class<T> clazz;
	
	private Finder<T, CT> finder;
	
	private Class<? extends Annotation> idType;
	
	public RetryObserverImpl(Class<T> clazz, Finder<T, CT> finder, Class<? extends Annotation> idType) {
		this.clazz = clazz;
		this.finder = finder;
		this.idType = idType;
	}
	
	/* (non-Javadoc)
	 * @see org.statefulj.fsm.RetryObserver#onRetry(java.lang.Object, java.lang.String, java.lang.Object[])
	 */
	@Override
	public T onRetry(T stateful, String event, Object... args) {
		
		T retVal = stateful;
		CT context = null;
		Object id = null;
		
		// Pull out the Context if available
		//
		context = getContext(context, args);
		
		// Fetch the ID value
		//
		id = getIdField(stateful, id);
		
		// Get a fresh copy of the StatefulEntity
		//
		retVal = findStatefulEntity(event, context, id);
		
		return retVal;
	}

	/**
	 * @param event
	 * @param context
	 * @param id
	 * @return
	 */
	private T findStatefulEntity(String event, CT context, Object id) {
		T retVal;
		if (id == null) {
			retVal = this.finder.find(clazz, event, context);
		} else {
			retVal = this.finder.find(clazz, id, event, context);
		}
		return retVal;
	}

	/**
	 * @param context
	 * @param args
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private CT getContext(CT context, Object... args) {
		if (args.length > 0 && (args[0] instanceof ContextWrapper<?>)) {
			ContextWrapper<CT> retryParms = (ContextWrapper<CT>)args[0];
			context = retryParms.getContext();
		}
		return context;
	}

	/**
	 * @param stateful
	 * @param id
	 * @return
	 */
	private Object getIdField(T stateful, Object id) {
		if (this.idType != null) {
			Field idField = ReflectionUtils.getReferencedField(stateful.getClass(), this.idType);
			if (idField != null) {
				idField.setAccessible(true);
				try {
					id = idField.get(stateful);
				} catch (IllegalArgumentException e) {
					logger.warn("Unable to locate id field for " + stateful);
				} catch (IllegalAccessException e) {
					logger.warn("Unable to locate id field for " + stateful);
				}
			}
		}
		return id;
	}

}
