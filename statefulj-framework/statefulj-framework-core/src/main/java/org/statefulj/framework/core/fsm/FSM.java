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
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;

/**
 * The Framework FSM.  The Framework FSM is responsible to extending the {@link org.statefulj.fsm.FSM}
 * to provide ability to autowire and reload Stateful Entities 
 * 
 * @author Andrew Hall
 *
 * @param <T> The Stateful Entity Type
 * @param <CT> The Context Type
 */
public class FSM<T, CT> extends org.statefulj.fsm.FSM<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(FSM.class);

	private Finder<T, CT> finder;

	private Class<T> clazz;
	
	private Class<? extends Annotation> idType;
	
	private ApplicationContext appContext;
	
	public FSM(
			String name, 
			Persister<T> persister, 
			int retryAttempts, 
			int retryInterval, 
			Class<T> clazz, 
			Class<? extends Annotation> idType,
			Finder<T, CT> finder,
			ApplicationContext applicationContext) {
		super(name, persister, retryAttempts, retryInterval);
		this.idType = idType;
		this.clazz = clazz;
		this.finder = finder;
		this.appContext = applicationContext;
	}

	@Override
	public State<T> onEvent(T stateful, String event, Object... parms)  throws TooBusyException {
		autowire(stateful);
		return super.onEvent(stateful, event, parms);
	}

	@Override
	protected State<T> transition(T stateful, State<T> current, String event, Transition<T> t, Object... args) throws RetryException {
		
		TransitionImpl<T> transition = (TransitionImpl<T>)t;
		StateActionPair<T> pair = transition.getStateActionPair(stateful);
		
		// If this transition is applicable to every state and doesn't cause a State change, don't bother
		// with setting the current state
		//
		if (!transition.isAny()) {
			setCurrent(stateful, current, pair.getState());
		}
		
		// Reloading MUST happen after we successful set the current state
		//
		if (transition.isReload()) {
			stateful = reload(stateful, event, args);
			autowire(stateful);
		}
		
		executeAction(
				pair.getAction(), 
				stateful, 
				event,
				current.getName(),
				pair.getState().getName(),
				args);
		
		return pair.getState();
	}
	
	private void autowire(T stateful) {
		// Autowire instantiated object 
		// TODO: Make this configurable - if using @Configurable - then this isn't necessary
		//
		this.appContext.getAutowireCapableBeanFactory().autowireBeanProperties(
				stateful,
			    AutowireCapableBeanFactory.AUTOWIRE_NO, 
			    false);
	}
	
	private T reload(T stateful, String event, Object... args) {
		
		T retVal = null;
		
		// Pull out the Context if available
		//
		CT context = getContext(args);
		
		// Fetch the ID value from the StatefulEntity
		//
		Object id = getId(stateful);
		
		// Get a fresh copy of the StatefulEntity
		//
		retVal = findStatefulEntity(event, context, id);
		
		// If we fetched a fresh instance, return it.  Otherwise,
		// return the current StatefulEntity.  Never pass back a null value
		//
		return (retVal != null) ? retVal : stateful;
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
	private CT getContext(Object... args) {
		CT context = null;
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
	private Object getId(T stateful) {
		Object id = null;
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
