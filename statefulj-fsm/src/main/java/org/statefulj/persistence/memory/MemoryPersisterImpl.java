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
	private State<T> start;
	private String stateFieldName;
    private volatile Field stateField;

	public MemoryPersisterImpl(final Collection<State<T>> states, final State<T> start) {
		setStart(start);
		setStates(states);
	}
	
	public MemoryPersisterImpl(List<State<T>> states, State<T> start, String stateFieldName) {
		this(states, start);
		this.stateFieldName = stateFieldName;
	}
	
	public MemoryPersisterImpl(T stateful, List<State<T>> states, State<T> start) {
		this(states, start);
		this.setCurrent(stateful, start);
	}
	
	public MemoryPersisterImpl(T stateful, List<State<T>> states, State<T> start, String stateFieldName) {
		this(states, start, stateFieldName);
		this.setCurrent(stateful, start);
	}
	
	public synchronized Collection<State<T>> getStates() {
		return states.values();
	}

	public synchronized State<T> addState(final State<T> state) {
		return states.put(state.getName(), state);
	}

	public State<T> removeState(final State<T> state) {
		return removeState(state.getName());
	}

	public synchronized State<T> removeState(final String name) {
		return states.remove(name);
	}

	public synchronized void setStates(final Collection<State<T>> states) {
		//Clear the map
		//
		this.states.clear();

		//Add new states
		//
		for(final State<T> state : states) {
			this.states.put(state.getName(), state);
		}
	}

	public State<T> getStart() {
		return start;
	}

	public void setStart(final State<T> start) {
		this.start = start;
	}

	public String getStateFieldName() {
		return stateFieldName;
	}

	public void setStateFieldName(String stateFieldName) {
		this.stateFieldName = stateFieldName;
	}

	public State<T> getCurrent(T stateful) {
		try {
			String key = (String)getStateField(stateful).get(stateful);
			State<T> state = (key != null) ? states.get(key) : null;
			return (state != null) ? state : this.start;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setCurrent(T stateful, State<T> current) {
		synchronized(stateful) {
			try {
				getStateField(stateful).set(stateful, current.getName());
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
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		synchronized(stateful) {
			if (this.getCurrent(stateful).equals(current)) {
				this.setCurrent(stateful, next);
			} else {
				throw new StaleStateException();
			}
		}
	}

	private Field getStateField(final T stateful) {
        if (stateField == null)
            stateField = locateStateField(stateful);
        return
            stateField;
	}

    private synchronized Field locateStateField(final T stateful) {
        Field field = null;

        // If a state field name was provided, retrieve by name
        //
        if (this.stateFieldName != null && !this.stateFieldName.equals("")) {
            try {
                field = stateful.getClass().getDeclaredField(this.stateFieldName);
            } catch (Exception e) {
                try {
                    field = stateful.getClass().getField(this.stateFieldName);
                } catch (Exception ee) {
                    // Ignore
                }
            }
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

        // Ensure that we can access the field
        //
        field.setAccessible(true);
        return field;
    }
}
