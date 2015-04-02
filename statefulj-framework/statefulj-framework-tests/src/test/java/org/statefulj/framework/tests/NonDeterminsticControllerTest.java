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
package org.statefulj.framework.tests;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;
import org.statefulj.fsm.TooBusyException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext-StatefulControllerTests.xml"})
public class NonDeterminsticControllerTest {
	
	@Resource
	UserRepository userRepo;
	
	@FSM("nonDetermisticController")
	StatefulFSM<User> nonDetermisticFSM;

	@Test
	public void testNonDeterministicTransitions() throws TooBusyException {

		final User user = userRepo.save(new User());

		String retVal = (String)nonDetermisticFSM.onEvent(user, "non-determinstic", true);
				
		assertEquals("onTrue", retVal);
		assertEquals(User.TWO_STATE, user.getState());

		retVal = (String)nonDetermisticFSM.onEvent(user, "non-determinstic", false);
		
		assertEquals("onFalse", retVal);
		assertEquals(User.THREE_STATE, user.getState());
	}
}
