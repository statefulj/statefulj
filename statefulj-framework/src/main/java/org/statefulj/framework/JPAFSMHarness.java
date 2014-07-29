package org.statefulj.framework;

import javax.transaction.Transactional;

import org.hibernate.ObjectNotFoundException;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

@Transactional
public class JPAFSMHarness extends FSMHarness {
	
	public JPAFSMHarness(FSM<Object> fsm, Class<?> clazz) {
		super(fsm, clazz);
	}
	
	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException, ObjectNotFoundException {
		return super.onEvent(event, id, parms);
	}

	public Object onEvent(String event, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException, ObjectNotFoundException {
		return super.onEvent(event, parms);
	}
}
