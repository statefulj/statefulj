package org.statefulj.webapp.controller;

import javax.annotation.Resource;

import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;

@StatefulController(
	clazz=User.class, 
	startState=UserController.NEW_STATE
)
public class UserController {
	
	// States
	//
	static final String NEW_STATE = "new";
	static final String NOT_NEW_STATE = "not-new";
	static final String BOO_STATE = "boo";
	static final String WHATEVER_STATE = "whatever";

	@Resource
	UserRepository userRepository;
	
	// Transitions/Actions
	//
	@Transition(from=NEW_STATE, event="springmvc:/new", to=NOT_NEW_STATE)
	public ModelAndView newUser(User user, String event) {
		userRepository.save(user);
		return userView(user, event);
	}

	@Transitions(value={
		@Transition(from=NOT_NEW_STATE, event="springmvc:/{id}/next", to=BOO_STATE),
		@Transition(from=BOO_STATE, event="springmvc:/{id}/next", to=BOO_STATE),
		@Transition(from=NOT_NEW_STATE, event="springmvc:/{id}/whatever", to=WHATEVER_STATE),
		@Transition(from=BOO_STATE, event="springmvc:/{id}/whatever", to=WHATEVER_STATE),
		@Transition(event="springmvc:/{id}/whatever"), // TODO : Broken
		@Transition(event="springmvc:/{id}/any"),
		@Transition(event="springmvc:post:/{id}/post")
	})
	public ModelAndView user(User user, String event) {
		return userView(user, event);
	}
	
	private ModelAndView userView(User user, String event) {
		ModelAndView mv = new ModelAndView("user");
		mv.addObject("user", user);
		mv.addObject("event", event);
		return mv;
	}
}
