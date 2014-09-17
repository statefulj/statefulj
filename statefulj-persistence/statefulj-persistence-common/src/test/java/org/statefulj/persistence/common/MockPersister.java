package org.statefulj.persistence.common;

import java.lang.reflect.Field;
import java.util.List;

import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

public class MockPersister<T> extends AbstractPersister<T> {

	public MockPersister(List<State<T>> states, String stateFieldName,
			State<T> start, Class<T> clazz) {
		super(states, stateFieldName, start, clazz);
	}

	@Override
	public void setCurrent(T stateful, State<T> current, State<T> next)
			throws StaleStateException {
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
