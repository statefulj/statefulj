/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.statefulj.framework.core.fsm;

import org.statefulj.fsm.Persister;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.RetryObserver;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;


public class FSM<T> extends org.statefulj.fsm.FSM<T> {

	public FSM(String name, Persister<T> persister, RetryObserver<T> retryObserver) {
		super(name, persister, retryObserver);
	}

	protected State<T> transition(T stateful, State<T> current, String event, Transition<T> transition, Object... args) throws RetryException {
		State<T> next = null;
		
		// If this is an "Any" transition - then skip checking for valid transition
		// just execute the Action
		//
		if (((TransitionImpl<T>)transition).isAny()) {
			StateActionPair<T> pair = transition.getStateActionPair(stateful);
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
