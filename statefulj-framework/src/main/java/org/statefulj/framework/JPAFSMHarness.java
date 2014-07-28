package org.statefulj.framework;

import javax.transaction.Transactional;

import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

public class JPAFSMHarness extends FSMHarness {
	
	public JPAFSMHarness(FSM<Object> fsm, Class<?> clazz) {
		super(fsm, clazz);
	}
	
	@Transactional
	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException {
		return super.onEvent(event, id, parms);
	}

	@Transactional
	public Object onEvent(String event, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException {
		return super.onEvent(event, parms);
	}
}
