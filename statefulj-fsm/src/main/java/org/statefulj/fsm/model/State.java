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

/**
 * Represents a State with the FSM.  Holds a map of events and {@link org.statefulj.fsm.model.Transition}s.
 *
 * @author Andrew Hall
 *
 * @param <T> The class of the Stateful Entity
 */
public interface State<T> {

	/**
	 * Name of the State.  This value is persisted as the State value in the Stateful Entity.
	 *
	 * @return Name of the State
	 */
	String getName();

	/**
	 * Returns the Transition for an Event
	 *
	 * @param event The event
	 * @return The Transition for this event
	 *
	 */
	Transition<T> getTransition(String event);

	/**
	 * Whether this State is an End State
	 *
	 * @return if true, then this is an End State
	 */
	boolean isEndState();

	/**
	 * Whether this is a Blocking State.  If Blocking, event will not process unless there is an explicit Transition for the
	 * event.  If blocked, the FSM will retry the event until the FSM transitions out of the blocked State
	 *
	 * @return if true, then State is a "blocking" state
	 */
	public boolean isBlocking();

	/**
	 * Set whether or not this is a Blocking State
	 *
	 * @param isBlocking if true, then this is a blocking State
	 */
	public void setBlocking(boolean isBlocking);

	/**
	 * Remove a Transition from the State
	 *
	 * @param event Remove the transition for this Event
	 */
	public void removeTransition(String event);

	/**
	 * Add a {@link org.statefulj.fsm.model.Transition}
	 *
	 * @param event The event to add the {@link org.statefulj.fsm.model.Transition}
	 * @param transition The {@link org.statefulj.fsm.model.Transition}
	 */
	public void addTransition(String event, Transition<T> transition);

	/**
	 * Add a deterministic Transition with an Action
	 *
	 * @param event The event to add the {@link org.statefulj.fsm.model.Transition}
	 * @param next The next State
	 * @param action The resulting {@link org.statefulj.fsm.model.Action}
	 */
	public void addTransition(String event, State<T> next, Action<T> action);

	/**
	 * Add a deterministic Transition with no Action
	 *
	 * @param event The event to add the {@link org.statefulj.fsm.model.Transition}
	 * @param next The next State
	 */
	public void addTransition(String event, State<T> next);

}
