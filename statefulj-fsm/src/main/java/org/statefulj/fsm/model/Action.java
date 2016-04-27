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
package org.statefulj.fsm.model;

import org.statefulj.fsm.RetryException;

/**
 * Interface for an Action Class.  The Action is invoked as part of a State Transition.
 *
 *
 * @author Andrew Hall
 *
 * @param <T> The class of the Stateful Entity
 */
public interface Action<T> {

	/**
	 * Called to execute an action based off a State Transition.
	 *
	 * @param stateful The Stateful Entity
	 * @param event The ocurring Event
	 * @param args A set of optional arguments passed into the onEvent method of the {@link org.statefulj.fsm.FSM}
	 * @throws RetryException thrown when the event must be retried due to Stale state or some other error condition
	 */
	void execute(T stateful, String event, Object ... args) throws RetryException;

}
