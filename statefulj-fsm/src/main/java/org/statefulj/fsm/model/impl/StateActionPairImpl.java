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
