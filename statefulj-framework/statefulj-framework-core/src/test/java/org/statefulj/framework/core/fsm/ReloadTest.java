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
package org.statefulj.framework.core.fsm;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.State;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * @author Andrew Hall
 *
 */
public class ReloadTest {
	
	static class Identifiable {
		
		@Id
		private Long id;
		
		public Identifiable(Long id) {
			this.id = id;
		}
		
		public Long getId() {
			return this.id;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testReload() throws RetryException {
		
		Identifiable value = new Identifiable(1L);

		Class<Identifiable> clazz = Identifiable.class;
		String event = "pow";
		Finder<Identifiable, Object> finder = mock(Finder.class);
		Object context = new Object();
		ContextWrapper<Object> cw = new ContextWrapper<Object>(context);
		
		when(finder.find(clazz, 1L, event, context)).thenReturn(value);
		
		State<Identifiable> from = mock(State.class);
		State<Identifiable> to = mock(State.class);
		Persister<Identifiable> persister = mock(Persister.class);

		TransitionImpl<Identifiable> transition = new TransitionImpl<Identifiable>(
				from,
				to, 
				event,
				null,
				false,
				true);
		
		FSM<Identifiable, Object> fsm = new FSM<Identifiable, Object>(
				"fsm", 
				persister, 
				1, 
				1, 
				Identifiable.class, 
				Id.class, 
				finder);
		
		fsm.transition(value, from, event, transition, cw);
		verify(finder).find(clazz, 1L, event, context);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void testRetryObserverWithoutContext() {
//		
//		Identifiable value1 = new Identifiable(1L);
//		Identifiable value2 = new Identifiable(1L);
//
//		Class<Identifiable> clazz = Identifiable.class;
//		String event = "pow";
//		Finder<Identifiable, Object> finder = mock(Finder.class);
//		
//		when(finder.find(clazz, 1L, event, null)).thenReturn(value2);
//
//		RetryObserver<Identifiable> retryObserver = new RetryObserverImpl<RetryObserverImplTest.Identifiable, Object>(clazz, finder, Id.class);
//		
//		Identifiable found = retryObserver.onRetry(value1, event);
//		
//		assertNotNull(found);
//		assertEquals(value2, found);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void testRetryObserverWithoutId() {
//		
//		Object value1 = new Object();
//		Object value2 = new Object();
//
//		Class<Object> clazz = Object.class;
//		String event = "pow";
//		Finder<Object, Object> finder = mock(Finder.class);
//		
//		when(finder.find(clazz, event, null)).thenReturn(value2);
//
//		RetryObserver<Object> retryObserver = new RetryObserverImpl<Object, Object>(clazz, finder, Id.class);
//		
//		Object found = retryObserver.onRetry(value1, event);
//		
//		assertNotNull(found);
//		assertEquals(value2, found);
	}

}
