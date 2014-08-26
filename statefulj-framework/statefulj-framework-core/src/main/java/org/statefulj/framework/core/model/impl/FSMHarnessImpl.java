package org.statefulj.framework.core.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableObject;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.PersistenceSupport;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class FSMHarnessImpl<T> implements FSMHarness {
	

	private PersistenceSupport<T> persistenceSupport;
	
	private FSM<T> fsm;
	
	private Class<T> clazz;
	
	public FSMHarnessImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			PersistenceSupport<T> persistenceSupport) {
		this.fsm = fsm;
		this.clazz = clazz;
		this.persistenceSupport = persistenceSupport;
	}
	
	@Override
	public T onEvent(String event, Object id, Object[] parms) throws TooBusyException {
		
		// Remove the first parameter from the parms - is the Id of the Entity Object
		//
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		ArrayList<Object> invokeParmlist = new ArrayList<Object>(parms.length + 1);
		
		T obj = null;
		
		if (id != null ){
			obj = this.persistenceSupport.find((Serializable)id);
			if (obj == null) {
				throw new RuntimeException("Unable to locate " + clazz.getSimpleName() + ", id=" + id);
			}
		} else {
			try {
				obj = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
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
	public T onEvent(String event, Object[] parms) throws TooBusyException {
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		Object id = parmList.remove(0);
		return onEvent(event, id, parmList.toArray());
	}

}
