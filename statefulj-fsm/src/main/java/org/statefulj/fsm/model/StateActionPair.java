package org.statefulj.fsm.model;

public interface StateActionPair<T> {
	
	State<T> getState();

	Action<T> getAction();
	
}
