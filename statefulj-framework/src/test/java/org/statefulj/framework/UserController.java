package org.statefulj.framework;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.statefulj.framework.annotations.StatefulController;
import org.statefulj.framework.annotations.Transition;

@Transactional
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

	@Transition(from=ONE_STATE, event="/first", to=TWO_STATE)
	public User oneToTwo(User user, String event) {
		userRepository.save(user);
		return user;
	}

	@Transition(from=TWO_STATE, event="/{id}/second", to=THREE_STATE)
	public User twoToThree(User user, String event) {
		return user;
	}
}
