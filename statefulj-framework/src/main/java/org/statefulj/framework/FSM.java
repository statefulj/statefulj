package org.statefulj.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;


public class FSM<T> extends org.statefulj.fsm.FSM<T> {

	Logger logger = LoggerFactory.getLogger(FSM.class);

	public FSM(Persister<T> persister) {
		super(persister);
	}

	public FSM(String name, Persister<T> persister) {
		super(name, persister);
	}
	
	public FSM(Persister<T> persister, int retries) {
		super(persister, retries);
	}

	protected State<T> transition(T stateful, State<T> current, String event, Transition<T> transition, Object... args) throws RetryException {
		State<T> next = null;
		
		// If this is an "Any" transition - then skip checking for valid transition
		// just execute the Action
		//
		if (((TransitionImpl<T>)transition).isAny()) {
			StateActionPair<T> pair = transition.getStateActionPair();
			executeAction(
					pair.getAction(), 
					stateful, 
					event,
					current.getName(),
					pair.getState().getName(),
					args);
			next = current;
		} else {
			next = super.transition(stateful, current, event, transition, args);
		}
		return next;
	}
}
