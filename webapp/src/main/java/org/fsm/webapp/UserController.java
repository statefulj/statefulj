package org.fsm.webapp;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;

@StatefulController(startState=UserController.NEW_STATE)
public class UserController {

	// States
	//
	public static final String NEW_STATE = "new";
	public static final String NOT_NEW_STATE = "not-new";
	public static final String BOO_STATE = "boo";
	public static final String WHATEVER_STATE = "whatever";
	
	// Transitions/Actions
	//
	@Transitions({
		@Transition(from=NEW_STATE, event="/{id}/new", to=NOT_NEW_STATE),
		@Transition(from=NEW_STATE, event="/{id}/show", to=NOT_NEW_STATE),
	})
	public String foo(Object stateful, String event) {
		return "foo";
	}

	@Transition(from=NOT_NEW_STATE, event="/{id}/new", to=BOO_STATE)
	public String boo(Object stateful, String event) {
		return "boo";
	}

	@Transition(from=NOT_NEW_STATE, event="/{id}/whatever", to=WHATEVER_STATE)
	public String whatever(Object stateful, String event) {
		return "whatever";
	}

	@Transition(from=BOO_STATE, event="/{id}/zoo", to=BOO_STATE)
	public String zoo(Object stateful, String event, HttpServletRequest request, @PathVariable(value="id")String id) {
		return "zoo";
	}
}
