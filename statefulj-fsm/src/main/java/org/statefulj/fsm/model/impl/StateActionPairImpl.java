package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;

public class StateActionPairImpl<T> implements StateActionPair<T> {

	State<T> state;
	Action<T> action;
	
	public StateActionPairImpl(State<T> state, Action<T> action) {
		this.state = state;
		this.action = action;
	}
	
	public State<T> getState() {
		return state;
	}
	
	public void setState(State<T> state) {
		this.state = state;
	}
	
	public Action<T> getAction() {
		return action;
	}
	
	public void setAction(Action<T> action) {
		this.action = action;
	}
	
}
