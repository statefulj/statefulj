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
