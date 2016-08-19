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

import java.lang.reflect.Field;
import java.util.List;

import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

public class MockPersister<T> extends AbstractPersister<T, String> {

	public MockPersister(List<State<T>> states, String stateFieldName,
			State<T> start, Class<T> clazz) {
		super(states, stateFieldName, start, clazz);
	}

	@Override
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
	}

	public Field getStateField() {
		return this.getStateFieldAccessor().getField();
	}

	@Override
	protected boolean validStateField(Field stateField) {
		return true;
	}

	@Override
	protected Field findIdField(Class<?> clazz) {
		Field idField = null;
		try {
			idField = clazz.getDeclaredField("idField");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return idField;
	}

	@Override
	protected Class<?> getStateFieldType() {
		return String.class;
	}

}
