package org.statefulj.fsm.model;

public interface State<T> {
	
	String getName();
	
	Transition<T> getTransition(String event);
	
	void addTransition(String event, Transition<T> transition);
	
	boolean isEndState();

}
