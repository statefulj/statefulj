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

import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.State;

import static org.mockito.Mockito.*;

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
		ApplicationContext appContext = mock(ApplicationContext.class);
		when(appContext.getAutowireCapableBeanFactory()).thenReturn(mock(AutowireCapableBeanFactory.class));

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
				appContext,
				finder);
		
		fsm.transition(value, from, event, transition, cw);
		verify(finder).find(clazz, 1L, event, context);
	}
}
