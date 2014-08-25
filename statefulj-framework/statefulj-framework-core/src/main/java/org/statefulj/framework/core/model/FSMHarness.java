package org.statefulj.framework.core.model;

import org.hibernate.ObjectNotFoundException;
import org.statefulj.fsm.TooBusyException;

public interface FSMHarness {

	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException, ObjectNotFoundException;

	public Object onEvent(String event, Object[] parms) throws TooBusyException, InstantiationException, IllegalAccessException, ObjectNotFoundException;

}
