package org.statefulj.framework.core.model;

import org.statefulj.fsm.TooBusyException;

public interface FSMHarness {

	public Object onEvent(String event, Object id, Object[] parms) throws TooBusyException;

	public Object onEvent(String event, Object[] parms) throws TooBusyException, InstantiationException;

}
