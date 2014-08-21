package org.statefulj.framework.controllers;

import javax.annotation.Resource;

import org.statefulj.framework.annotations.StatefulController;
import org.statefulj.framework.annotations.Transition;
import org.statefulj.framework.dao.UserRepository;
import org.statefulj.framework.model.User;

@StatefulController(
	clazz=User.class, 
	startState=UserController.ONE_STATE
)
public class UserController {
	
	@Resource
	UserRepository userRepository;
	
	// States
	//
	public static final String ONE_STATE = "one";
	public static final String TWO_STATE = "two";
	public static final String THREE_STATE = "three";
	
	@Transition(from=ONE_STATE, event="springmvc:get:/first", to=TWO_STATE)
	public User oneToTwo(User user, String event) {
		userRepository.save(user);
		return user;
	}

	@Transition(from=TWO_STATE, event="springmvc:post:/{id}/second", to=THREE_STATE)
	public User twoToThree(User user, String event) {
		return user;
	}

	@Transition(event="springmvc:/{id}/any")
	public User any(User user, String event) {
		return user;
	}
}
