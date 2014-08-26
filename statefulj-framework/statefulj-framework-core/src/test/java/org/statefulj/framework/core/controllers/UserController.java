package org.statefulj.framework.core.controllers;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.model.User;

@StatefulController(
	clazz=User.class, 
	startState=UserController.ONE_STATE,
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
