package org.statefulj.framework.persistence.jpa;

import javax.transaction.Transactional;

import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.framework.core.model.impl.StatefulFSMImpl;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.TooBusyException;

@Transactional
public class JPAStatefulFSMImpl<T> extends StatefulFSMImpl<T> {
	
	public JPAStatefulFSMImpl(
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
