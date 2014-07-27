package org.statefulj.webapp;

import java.util.ArrayList;
import java.util.Arrays;

import javax.transaction.Transactional;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.repository.support.DomainClassConverter;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;

public class FSMHarness {
	

	@Autowired
	private DomainClassConverter<?> domainClassConverter;
	
	@Autowired
	UserRepository userRepository;

	private FSM<Object> fsm;
	
	private Class<?> clazz;
	
	public FSMHarness(FSM<Object> fsm, Class<?> clazz) {
		this.fsm = fsm;
		this.clazz = clazz;
	}
	
	public Object onEvent(String event, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException {
		
		// Remove the first parameter from the parms - is the Id of the Entity Object
		//
		ArrayList<Object> parmList = new ArrayList<Object>(Arrays.asList(parms));
		ArrayList<Object> invokeParmlist = new ArrayList<Object>(parms.length);
		Object id = parmList.remove(0);
		
		Object obj = this.domainClassConverter.convert(id, TypeDescriptor.forObject(id), TypeDescriptor.valueOf(clazz));
		if (obj == null) {
			obj = clazz.newInstance();
		}
		
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
		fsm.onEvent(obj, event, invokeParmlist.toArray());
		return returnValue.getValue();
	}

}
