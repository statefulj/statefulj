package org.statefulj.framework.core.model.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class FSMHarnessImpl<T, CT> implements FSMHarness {
	
	private Logger logger = LoggerFactory.getLogger(FSMHarnessImpl.class);
	
	private Factory<T, CT> factory;
	
	private Finder<T, CT> finder;
	
	private FSM<T> fsm;
	
	private Class<T> clazz;
	
	public FSMHarnessImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			Factory<T, CT> factory,
			Finder<T, CT> finder) {
		this.fsm = fsm;
		this.clazz = clazz;
		this.factory = factory;
		this.finder = finder;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException {
		
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		CT context = (parmList.size() > 0) ? (CT)parmList.remove(0) : null;
		
		ArrayList<Object> invokeParmlist = new ArrayList<Object>(parms.length + 1);
		
		T obj = null;
		
		if (id == null) {
			obj = this.finder.find(clazz, event, context);
		} else {
			obj = this.finder.find(clazz, id, event, context);
		}

		if ( obj == null ) {
			if (id != null) {
				logger.error("Unable to locate object of type {}, id={}, event={}", clazz.getName(), id, event);
				throw new RuntimeException("Unable to locate object of type " + clazz.getName() + ", id=" + ((id == null) ? "null" : id) + ", event=" + event);
			} else {
				obj = this.factory.create(this.clazz, event, context);
				if (obj == null) {
					logger.error("Unable to create object of type {}, event={}", clazz.getName(), event);
					throw new RuntimeException("Unable to create object of type " + clazz.getName() + ", event=" + event);
				}
			}
		}
		
		// Create a Mutable Object and add it to the Parameter List - it will be used
		// to return the returned value from the Controller as the FSM returns the State
		//
		MutableObject<T> returnValue = new MutableObject<T>();
		invokeParmlist.add(returnValue);
		invokeParmlist.addAll(parmList);
		
		// Call the FSM
		// 
		fsm.onEvent(obj, event, invokeParmlist.toArray());
		return returnValue.getValue();
	}
	
	@Override
	public Object onEvent(String event, Object[] parms) throws TooBusyException {
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		Object id = parmList.remove(0);
		return onEvent(event, id, parmList.toArray());
	}

}
