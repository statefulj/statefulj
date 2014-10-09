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
package org.statefulj.fsm;

import org.statefulj.fsm.model.State;

/**
 * A Persister is responsible for maintaining the persistence of the current State of the
 * FSM
 * 
 * @author Andrew Hall
 *
 */
public interface Persister<T> {
	
	/**
	 * Returns the current state.  This will not call into the 
	 * underlying database
	 * 
	 * @param id of the Object
	 * @return current State
	 */
	State<T> getCurrent(T obj);

	/**
	 * Set the current state to the next state.  Will serialize access to the persistence
	 * and ensure that expected current state is indeed the current state.  If not, will throw
	 * a StaleStateException
	 * 
	 * @param id of the Object
	 * @param current
	 * @param next
	 * @throws StaleStateException
	 */
	void setCurrent(T obj, State<T> current, State<T> next) throws StaleStateException;
}
