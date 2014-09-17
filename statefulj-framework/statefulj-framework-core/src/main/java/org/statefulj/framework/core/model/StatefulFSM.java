package org.statefulj.framework.core.model;

import org.statefulj.fsm.TooBusyException;

public interface StatefulFSM<T> {
	
	Object onEvent(T stateful, String event, Object... parms)  throws TooBusyException ;

}
