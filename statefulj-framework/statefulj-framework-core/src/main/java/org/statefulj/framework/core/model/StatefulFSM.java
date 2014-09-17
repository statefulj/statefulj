package org.statefulj.framework.core.model;

import org.statefulj.fsm.TooBusyException;

public interface StatefulFSM<T> {
	
	Object onEvent(String event, Object... parms)  throws TooBusyException ;

	Object onEvent(T stateful, String event, Object... parms)  throws TooBusyException ;

}
