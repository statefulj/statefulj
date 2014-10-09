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
package org.statefulj.framework.tests.controllers;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;
import static org.statefulj.framework.tests.model.User.*;

@StatefulController(
	clazz=User.class, 
	startState=ONE_STATE,
	blockingStates={SIX_STATE},
	noops={
		@Transition(event="springmvc:/{id}/four", to=FOUR_STATE),
		@Transition(event="five", to=FIVE_STATE),
		@Transition(event="camel:six", to=SIX_STATE),
		@Transition(event="unblock", to=SEVEN_STATE),
	}
)
public class UserController {
	
	@Resource
	UserRepository userRepository;
	
	@Transition(from=ONE_STATE, event="springmvc:get:/first", to=TWO_STATE)
	public User oneToTwo(User user, String event) {
		userRepository.save(user);
		return user;
	}

	@Transition(from=TWO_STATE, event="springmvc:post:/{id}/second", to=THREE_STATE)
	public User twoToThree(User user, String event) {
		return user;
	}

	@Transition(from=THREE_STATE, event="springmvc:post:/{id}/second")
	public User threeToThree(User user, String event) {
		return user;
	}

	@Transition(event="springmvc:/{id}/any")
	public User any(User user, String event) {
		return user;
	}
	
	@Transition(event="camel:camelOne")
	public void camelOne(User user, String event, Long id) {
	}
	
	@Transition(event="camel:camelTwo")
	public void camelTwo(User user, String event, Long id) {
	}
	
	@ExceptionHandler(Exception.class)
	public String handleError(Exception e) {
		return "called";
	}
}
