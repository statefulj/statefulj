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

package org.statefulj.persistence.common;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.common.utils.FieldAccessor;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.StateFieldAccessor;

public abstract class AbstractPersister<T, ID> implements Persister<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPersister.class);

	private FieldAccessor<T, ID> idFieldAccessor;
	private StateFieldAccessor<T> stateFieldAccessor;
	private State<T> startState;
	private Class<T> clazz;
	private HashMap<String, State<T>> states = new HashMap<String, State<T>>();

	public AbstractPersister(
			List<State<T>> states,
			String stateFieldName,
			State<T> startState,
			Class<T> clazz) {

		this.clazz = clazz;

		// Find the Id field of the Entity
		//
		Field idField = findIdField(clazz);

		if (idField == null) {
			throw new RuntimeException("No Id field defined");
		}

		this.idFieldAccessor = buildIdFieldAccessor(idField, clazz);

		// Find the State field of the Entity
		//
		Field stateField = findStateField(stateFieldName, clazz);

		if (stateField == null) {
			throw new RuntimeException("No State field defined");
		}

		if (!validStateField(stateField)) {
			throw new RuntimeException(
					String.format(
							"State field, %s, of class %s, is not of type %s",
							stateField.getName(),
							clazz,
							getStateFieldType()));
		}
		this.stateFieldAccessor = buildStateFieldAccessor(stateField, clazz);

		// Start state - returned when no state is set
		//
		this.startState = startState;

		// Index States into a HashMap
		//
		for(State<T> state : states) {
			this.states.put(state.getName(), state);
		}
	}

	@Override
	public State<T> getCurrent(T stateful) {
		State<T> state;
		try {
			Serializable stateKey = this.getState(stateful);
			state = (stateKey == null) ? this.startState : this.states.get(stateKey);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		state = (state == null) ? this.startState : state;
		return state;
	}

	/**
	 * Set the current State.  This method will ensure that the state in the db matches the expected current state.
	 * If not, it will throw a StateStateException
	 *
	 * @param stateful Stateful Entity
	 * @param current Expected current State
	 * @param next The value of the next State
	 * @throws StaleStateException thrown if the value of the State does not equal to the provided current State
	 */
	@Override
	public abstract void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException;

	@Override
	public void setStates(Collection<State<T>> states) {
		this.states.clear();

		for(State<T> state : states) {
			this.states.put(state.getName(), state);
		}
	}

	@Override
	public void setStartState(State<T> startState) {
		this.startState = startState;
	}

	public Field getIdField() {
		return this.idFieldAccessor.getField();
	}

	public Field getStateField() {
		return this.stateFieldAccessor.getField();
	}

	protected abstract boolean validStateField(Field stateField);

	protected abstract Field findIdField(Class<?> clazz);

	protected Field findStateField(String stateFieldName, Class<?> clazz) {
		Field stateField = null;
		if (StringUtils.isEmpty(stateFieldName)) {
			stateField = ReflectionUtils.getFirstAnnotatedField(clazz, org.statefulj.persistence.annotations.State.class);
		} else {
			try {
				stateField = clazz.getDeclaredField(stateFieldName);
			} catch (NoSuchFieldException e) {
				logger.error("Unable to locate state field for {}, stateFieldName={}", clazz.getName(), stateFieldName);
			} catch (SecurityException e) {
				logger.error("Security exception trying to locate state field for {}, stateFieldName={}", clazz.getName(), stateFieldName);
				logger.error("Exception", e);
			}
		}
		return stateField;
	}

	protected FieldAccessor<T, ID> buildIdFieldAccessor(Field idField, Class<T> clazz) {
		return new FieldAccessor<T, ID>(clazz, idField);
	}

	protected StateFieldAccessor<T> buildStateFieldAccessor(Field stateField, Class<T> clazz) {
		return new StateFieldAccessor<T>(clazz, stateField);
	}

	protected abstract Class<?> getStateFieldType();

	protected State<T> getStartState() {
		return startState;
	}

	protected Class<T> getClazz() {
		return clazz;
	}

	protected Object getId(T obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		return this.idFieldAccessor.getValue(obj);
	}

	protected Serializable getState(T obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		return this.stateFieldAccessor.getValue(obj);
	}

	protected void setState(T obj, String state) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		state = (state == null) ? this.startState.getName() : state;
		this.stateFieldAccessor.setValue(obj, state);
	}

	protected void throwStaleState(State<T> current, State<T> next) throws StaleStateException {
		String err = String.format(
				"Unable to update state, entity.state=%s, db.state=%s",
				current.getName(),
				next.getName());
		throw new StaleStateException(err);
	}

	protected StateFieldAccessor<T> getStateFieldAccessor() {
		return stateFieldAccessor;
	}

	protected FieldAccessor<T, ID> getIdFieldAccessor() {
		return idFieldAccessor;
	}
}
