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
 * A Transition is a "reaction" to an Event based off the {@link org.statefulj.fsm.model.State} of the Stateful Entity.
 * It is comprised of an optional next State value and an optional {@link org.statefulj.fsm.model.Action}
 *
 * @author Andrew Hall
 *
 * @param <T> The class of the Stateful Entity
 */
public interface Transition<T> {

	/**
	 * Return the {@link org.statefulj.fsm.model.StateActionPair}
	 *
	 * @param stateful the Stateful Entity
	 *
	 * @return the {@link org.statefulj.fsm.model.StateActionPair}
	 * @param event The occurring Event
	 * @param args Optional parameters that was passed into the FSM
	 *
	 * @throws RetryException is thrown if there is an error determining the next State and Action and the FSM should
	 *         re-process the event
	 */
	StateActionPair<T> getStateActionPair(T stateful, String event, Object ... args) throws RetryException;
}
