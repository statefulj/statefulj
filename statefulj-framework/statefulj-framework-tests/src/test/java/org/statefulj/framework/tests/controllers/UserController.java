package org.statefulj.framework.tests.controllers;

import javax.annotation.Resource;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;

@StatefulController(
	clazz=User.class, 
	startState=UserController.ONE_STATE,
	noops={
		@Transition(event="springmvc:/{id}/four", to=UserController.FOUR_STATE),
		@Transition(event="five", to=UserController.FIVE_STATE)
	}
)
public class UserController {
	
	@Resource
	UserRepository userRepository;
	
	// States
	//
	public static final String ONE_STATE = "one";
	public static final String TWO_STATE = "two";
	public static final String THREE_STATE = "three";
	public static final String FOUR_STATE = "four";
	public static final String FIVE_STATE = "five";
	
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
}
