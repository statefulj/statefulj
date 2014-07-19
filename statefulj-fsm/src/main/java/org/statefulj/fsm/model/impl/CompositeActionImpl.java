package org.statefulj.fsm.model.impl;

import java.util.List;

import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.Action;

public class CompositeActionImpl<T> implements Action<T> {

	List<Action<T>> actions;
	
	public CompositeActionImpl(List<Action<T>> actions) {
		this.actions = actions;
	}
	
	public void execute(T stateful, String event, Object ... args) throws RetryException{
		for(Action<T> action : this.actions) {
			action.execute(stateful, event, args);
		}
	}

}
