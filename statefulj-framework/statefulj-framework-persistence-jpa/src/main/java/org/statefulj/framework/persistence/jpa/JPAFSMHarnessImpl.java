package org.statefulj.framework.persistence.jpa;

import javax.transaction.Transactional;

import org.hibernate.ObjectNotFoundException;
import org.statefulj.framework.core.model.PersistenceSupport;
import org.statefulj.framework.core.model.impl.FSMHarnessImpl;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

@Transactional
public class JPAFSMHarnessImpl<T> extends FSMHarnessImpl<T> {
	
	public JPAFSMHarnessImpl(
			FSM<T> fsm, 
			Class<T> clazz, 
			PersistenceSupport<T> persistenceSupport) {
		super(fsm, clazz, persistenceSupport);
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
