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
package org.statefulj.framework.tests.ddd;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.fsm.TooBusyException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.statefulj.framework.tests.utils.ReflectionUtils.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-DomainEntityTests.xml"})
public class DomainEntityTest {
	
	@FSM
	StatefulFSM<DomainEntity> fsm;
	
	@Resource
	ApplicationContext appContext;

	@Test
	public void testDomainEntityFSM() throws TooBusyException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		ReferenceFactory refFactory = new ReferenceFactoryImpl("domainEntity");
		
		// Make sure proxy is constructed
		//
		Object mvcBinder = this.appContext.getBean(refFactory.getBinderId("springmvc"));

		// Verify new User scenario
		//
		HttpServletRequest context = mock(HttpServletRequest.class);		int value = 1;
		DomainEntity entity = invoke(mvcBinder, "$_get_event-x", DomainEntity.class, context, 1);
		
		assertNotNull(entity);
		assertEquals(1, entity.getValue());
		assertEquals(DomainEntity.STATE_B, entity.getState());
		
		entity.onEventY(2);
		assertEquals(2, entity.getValue());
		assertEquals(DomainEntity.STATE_A, entity.getState());
	}
	
}
