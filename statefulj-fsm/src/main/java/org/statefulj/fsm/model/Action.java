package org.statefulj.fsm.model;

import org.statefulj.fsm.RetryException;

public interface Action<T> {
	
	void execute(T stateful, String event, Object ... args) throws RetryException;

}
