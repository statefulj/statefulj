package org.statefulj.persistence.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.statefulj.fsm.model.State;

import static org.mockito.Mockito.*;

public class AbstractPersisterTest {
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAlternateStateFields() {
		
		List<State<MockEntity>> states = new ArrayList<State<MockEntity>>();
		State<MockEntity> startState = mock(State.class);
		states.add(startState);
		
		MockPersister<MockEntity> mockPersister = new MockPersister<MockEntity>(
				states, 
				"stateField1", 
				startState, 
				MockEntity.class);
		Field field = mockPersister.getStateField();
		
		assertNotNull(field);
		assertEquals("stateField1", field.getName());

		mockPersister = new MockPersister<MockEntity>(
				states, 
				"", 
				startState, 
				MockEntity.class);
		field = mockPersister.getStateField();
		
		assertNotNull(field);
		assertEquals("stateField2", field.getName());
	}

}
