package org.statefulj.webapp;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.statefulj.framework.annotations.StatefulController;
import org.statefulj.framework.annotations.Transition;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;

@StatefulController(
	clazz=User.class, 
	startState=UserController.NEW_STATE
)
@Transactional
public class UserController {
	
	// States
	//
	public static final String NEW_STATE = "new";
	public static final String NOT_NEW_STATE = "not-new";
	public static final String BOO_STATE = "boo";
	public static final String WHATEVER_STATE = "whatever";

	@Autowired
	UserRepository userRepository;
	
	// Transitions/Actions
	//
	@Transition(from=NEW_STATE, event="/new", to=NOT_NEW_STATE)
	public ModelAndView newUser(User user, String event) {
		userRepository.save(user);
		ModelAndView mv = new ModelAndView("new");
		mv.addObject("id", user.getId());
		return mv;
	}

	@Transition(from=NOT_NEW_STATE, event="/{id}/next", to=BOO_STATE)
	public String boo(User user, String event) {
		return "boo";
	}

	@Transition(from=NOT_NEW_STATE, event="/{id}/whatever", to=WHATEVER_STATE)
	public String whatever(User user, String event) {
		return "whatever";
	}

	@Transition(from=BOO_STATE, event="/{id}/next", to=BOO_STATE)
	public String zoo(User user, String event) {
		return "zoo";
	}
}
