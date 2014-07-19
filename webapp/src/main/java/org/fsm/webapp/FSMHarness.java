package org.fsm.webapp;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableObject;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class FSMHarness {
	
	private FSM<Object> fsm;
	
	private Object stateful = new Object();

	public FSMHarness(FSM<Object> fsm, Object stateful) {
		this.fsm = fsm;
		this.stateful = stateful;
	}
	
	public FSM<Object> getFsm() {
		return fsm;
	}

	public void setFsm(FSM<Object> fsm) {
		this.fsm = fsm;
	}

	public Object onEvent(String event, Object[] parms) throws TooBusyException {
		
		// Remove the first parameter from the parms - is the Id of the Entity Object
		//
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		ArrayList<Object> invokeParmlist = new ArrayList<Object>(parms.length);
		Object id = parmList.remove(0);
		
		// Create a Mutable Object and add it to the Parmater List - it will be used
		// to return the returned value from the Controller as the FSM returns the State
		//
		MutableObject<Object> returnValue = new MutableObject<Object>();
		invokeParmlist.add(returnValue);
		invokeParmlist.addAll(parmList);
		
		// Call the FSM
		// 
		// TODO : Remove the placeholder stateful object with a call to the Spring Data Repo
		//        to create/fetch the object
		fsm.onEvent(this.stateful, event, invokeParmlist.toArray());
		return returnValue.getValue();
	}

}
