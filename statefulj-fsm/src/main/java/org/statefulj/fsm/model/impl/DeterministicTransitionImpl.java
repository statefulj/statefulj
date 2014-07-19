package org.statefulj.fsm.model.impl;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;

public class DeterministicTransitionImpl<T> implements Transition<T> {
	
	private StateActionPair<T> stateActionPair;
	
	public DeterministicTransitionImpl(State<T> from, State<T> to, String event) {
		stateActionPair = new StateActionPairImpl<T>(to, null);
		from.addTransition(event, this);
	}

	public DeterministicTransitionImpl(State<T> from, State<T> to, String event, Action<T> action) {
		stateActionPair = new StateActionPairImpl<T>(to, action);
		from.addTransition(event, this);
	}

	public DeterministicTransitionImpl(State<T> to, Action<T> action) {
		stateActionPair = new StateActionPairImpl<T>(to, action);
	}

	public DeterministicTransitionImpl(State<T> to) {
		stateActionPair = new StateActionPairImpl<T>(to, null);
	}

	public StateActionPair<T> getStateActionPair() {
		return stateActionPair;
	}

	public void setStateActionPair(StateActionPair<T> stateActionPair) {
		this.stateActionPair = stateActionPair;
	}

}
