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
package org.statefulj.framework.core.controllers;

import org.statefulj.framework.core.annotations.FSM;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.User;

@StatefulController(
	clazz=User.class, 
	startState=UserController.ONE_STATE,
	blockingStates={UserController.FIVE_STATE},
	noops={
		@Transition(event="mock:four", to=UserController.FOUR_STATE),
		@Transition(event="five", to=UserController.FIVE_STATE)
	}
)
public class UserController {
	
	// States
	//
	public static final String ONE_STATE = "one";
	public static final String TWO_STATE = "two";
	public static final String THREE_STATE = "three";
	public static final String FOUR_STATE = "four";
	public static final String FIVE_STATE = "five";
	
	@FSM
	StatefulFSM<User> fsm;
	
	@Transition(from=ONE_STATE, event="mock:one", to=TWO_STATE)
	public User oneToTwo(User user, String event) {
		return user;
	}

	@Transition(from=TWO_STATE, event="two", to=THREE_STATE)
	public User twoToThree(User user, String event) {
		return user;
	}

	@Transition(event="any")
	public User any(User user, String event) {
		return user;
	}
}
