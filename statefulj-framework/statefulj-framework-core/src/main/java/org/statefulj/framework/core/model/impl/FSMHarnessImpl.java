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
package org.statefulj.framework.core.model.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.core.fsm.ContextWrapper;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.fsm.TooBusyException;

public class FSMHarnessImpl<T, CT> implements FSMHarness {
	
	private static final Logger logger = LoggerFactory.getLogger(FSMHarnessImpl.class);
	
	private Factory<T, CT> factory;
	
	private Finder<T, CT> finder;
	
	private StatefulFSM<T> fsm;
	
	private Class<T> clazz;
	
	public FSMHarnessImpl(
			StatefulFSM<T> fsm, 
			Class<T> clazz, 
			Factory<T, CT> factory,
			Finder<T, CT> finder) {
		this.fsm = fsm;
		this.clazz = clazz;
		this.factory = factory;
		this.finder = finder;
	}
	
	@Override
	@SuppressWarnings({ "unchecked" })
	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException {
		
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		CT context = (parmList.size() > 0) ? (CT)parmList.remove(0) : null;
		ContextWrapper<CT> retryParms = new ContextWrapper<CT>(context);
		parmList.add(0, retryParms);
		
		T stateful = null;
		
		if (id == null) {
			stateful = findStateful(event, context);
		} else {
			stateful = findStateful(event, id, context);
		}

		if (stateful == null) {
			if (id != null) {
				logger.error("Unable to locate object of type {}, id={}, event={}", clazz.getName(), id, event);
				throw new RuntimeException("Unable to locate object of type " + clazz.getName() + ", id=" + ((id == null) ? "null" : id) + ", event=" + event);
			} else {
				stateful = this.factory.create(this.clazz, event, context);
				if (stateful == null) {
					logger.error("Unable to create object of type {}, event={}", clazz.getName(), event);
					throw new RuntimeException("Unable to create object of type " + clazz.getName() + ", event=" + event);
				}
			}
		}
		
		return fsm.onEvent(stateful, event, parmList.toArray());
	}

	@Override
	public Object onEvent(String event, Object[] parms) throws TooBusyException {
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		Object id = parmList.remove(0);
		return onEvent(event, id, parmList.toArray());
	}

	/**
	 * @param event
	 * @param id
	 * @param context
	 * @return
	 */
	private T findStateful(String event, Object id, CT context) {
		return this.finder.find(clazz, id, event, context);
	}

	/**
	 * @param event
	 * @param context
	 * @return
	 */
	private T findStateful(String event, CT context) {
		return this.finder.find(clazz, event, context);
	}
	
}
