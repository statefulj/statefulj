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
package org.statefulj.persistence.memory;

import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.StateFieldAccessor;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread safe, in memory Persister.
 *
 * @author Andrew Hall
 *
 */
public class MemoryPersisterImpl<T> implements Persister<T> {

	private final Map<String, State<T>> states = new HashMap<String, State<T>>();
	private State<T> startState;
	private String stateFieldName;
	private StateFieldAccessor<T> statePersister;

	public MemoryPersisterImpl() {}

	public MemoryPersisterImpl(final Collection<State<T>> states, final State<T> startState) {
		setStartState(startState);
		setStates(states);
	}

	public MemoryPersisterImpl(List<State<T>> states, State<T> startState, String stateFieldName) {
		this(states, startState);
		this.stateFieldName = stateFieldName;
	}

	public MemoryPersisterImpl(T stateful, List<State<T>> states, State<T> startState) {
		this(states, startState);
		this.setCurrent(stateful, startState);
	}

	public MemoryPersisterImpl(T stateful, List<State<T>> states, State<T> startState, String stateFieldName) {
		this(states, startState, stateFieldName);
		this.setCurrent(stateful, startState);
	}

	public synchronized Collection<State<T>> getStates() {
		return states.values();
	}

	public synchronized State<T> addState(final State<T> state) {
		return states.put(state.getName(), state);
	}

	public synchronized State<T> removeState(final State<T> state) {
		return removeState(state.getName());
	}

	public synchronized State<T> removeState(final String name) {
		return states.remove(name);
	}

	@Override
	public synchronized void setStates(final Collection<State<T>> states) {
		//Clear the map
		//
		this.states.clear();

		//Add new states
		//
		for(State state : states) {
			this.states.put(state.getName(), state);
		}
	}

	public State<T> getStartState() {
		return startState;
	}

	@Override
	public void setStartState(final State<T> startState) {
		this.startState = startState;
	}

	public String getStateFieldName() {
		return stateFieldName;
	}

	@Override
	public State<T> getCurrent(T stateful) {
		try {
			String key = this.getStateFieldAccessor(stateful).getValue(stateful);
			State<T> state = (key != null) ? states.get(key) : null;
			return (state != null) ? state : this.startState;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setCurrent(T stateful, State<T> current) {
		synchronized(stateful) {
			try {
				this.getStateFieldAccessor(stateful).setValue(stateful, current.getName());
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/*
	 * Serialize all update of state.  Ensure that the current state is the same State that
	 * was evaluated. If not, throw an exception
	 *
	 * (non-Javadoc)
	 * @see org.fsm.Persister#setCurrent(org.fsm.model.State, org.fsm.model.State)
	 */
	@Override
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		synchronized(stateful) {
			if (this.getCurrent(stateful).equals(current)) {
				this.setCurrent(stateful, next);
			} else {
				throw new StaleStateException();
			}
		}
	}

	private StateFieldAccessor<T> getStateFieldAccessor(final T stateful) {
		if (this.statePersister == null)
			initStateFieldAccessor(stateful);
		return this.statePersister;
	}

	private synchronized void initStateFieldAccessor(T stateful) {
		if (this.statePersister == null) {
			Field stateField = locateStateField(stateful);
			this.statePersister = new StateFieldAccessor(stateful.getClass(), stateField);
		}
	}

	private synchronized Field locateStateField(final T stateful) {
		Field field;

		// If a state field name was provided, retrieve by name
		//
		if (this.stateFieldName != null && !this.stateFieldName.equals("")) {
			field =
					ReflectionUtils.getField(stateful.getClass(), this.stateFieldName);
		}


		// Else, fetch the field by Annotation
		//
		else {
			field = ReflectionUtils.getFirstAnnotatedField(stateful.getClass(), org.statefulj.persistence.annotations.State.class);
			if (field != null) {
				this.stateFieldName = field.getName();
			}
		}

		if (field == null) {
			throw new RuntimeException("Unable to locate a State field for stateful: " + stateful);
		}

		return field;
	}
}
