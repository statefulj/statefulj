package org.statefulj.framework.model.impl;

import javax.transaction.Transactional;

import org.hibernate.ObjectNotFoundException;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

@Transactional
public class JPAFSMHarnessImpl extends FSMHarnessImpl {
	
	public JPAFSMHarnessImpl(FSM<Object> fsm, Class<?> clazz) {
		super(fsm, clazz);
	}
	
	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException, ObjectNotFoundException {
		return super.onEvent(event, id, parms);
	}

	public Object onEvent(String event, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException, ObjectNotFoundException {
		return super.onEvent(event, parms);
	}
}
