package org.statefulj.framework.persistence.jpa;

import javax.transaction.Transactional;

import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.impl.FSMHarnessImpl;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

@Transactional
public class JPAFSMHarnessImpl<T> extends FSMHarnessImpl<T> {
	
	public JPAFSMHarnessImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			Factory<T> factory,
			Finder<T> finder) {
		super(fsm, clazz, factory, finder);
	}
	
	@Override
	public T onEvent(String event, Object id, Object[] parms) throws TooBusyException {
		return super.onEvent(event, id, parms);
	}

	@Override
	public T onEvent(String event, Object[] parms) throws TooBusyException {
		return super.onEvent(event, parms);
	}
}
