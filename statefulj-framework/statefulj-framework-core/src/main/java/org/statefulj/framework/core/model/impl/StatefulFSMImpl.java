package org.statefulj.framework.core.model.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class StatefulFSMImpl<T> implements StatefulFSM<T> {
	
	private Logger logger = LoggerFactory.getLogger(StatefulFSMImpl.class);
	
	private FSM<T> fsm;
	
	private Factory<T, ?> factory;
	
	private Class<T> clazz;
	
	public StatefulFSMImpl(
			FSM<T> fsm, 
			Class<T> clazz,
			Factory<T, ?> factory) {
		this.fsm = fsm;
		this.clazz = clazz;
		this.factory = factory;
	}
	
	@Override
	public Object onEvent(String event, Object... parms) throws TooBusyException {
		T stateful = this.factory.create(this.clazz, event, null);
		if (stateful == null) {
			logger.error("Unable to create object of type {}, event={}", clazz.getName(), event);
			throw new RuntimeException("Unable to create object of type " + clazz.getName() + ", event=" + event);
		}
		return onEvent(stateful, event, parms);
	}

	@Override
	public Object onEvent(T stateful, String event, Object... parms) throws TooBusyException {
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		ArrayList<Object> invokeParmlist = new ArrayList<Object>(parms.length + 1);
		
		
		// Create a Mutable Object and add it to the Parameter List - it will be used
		// to return the returned value from the Controller as the FSM returns the State
		//
		MutableObject<T> returnValue = new MutableObject<T>();
		invokeParmlist.add(returnValue);
		invokeParmlist.addAll(parmList);
		
		// Call the FSM
		// 
		fsm.onEvent(stateful, event, invokeParmlist.toArray());
		return returnValue.getValue();
	}

}
