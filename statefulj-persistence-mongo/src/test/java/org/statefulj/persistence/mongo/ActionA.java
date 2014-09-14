package org.statefulj.persistence.mongo;

import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.Action;

public class ActionA implements Action<Order> {

	public void execute(Order order, String event, Object... args) throws RetryException {
	}

}
