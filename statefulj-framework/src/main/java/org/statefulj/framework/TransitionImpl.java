package org.statefulj.framework;

import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.impl.DeterministicTransitionImpl;

public class TransitionImpl<T> extends DeterministicTransitionImpl<T> {
	
	private boolean any = false;

	public TransitionImpl(
			State<T> from, 
			State<T> to, 
			String event,
			Action<T> action,
			boolean any) {
		super(from, to, event, action);
		this.any = any;
	}

	public boolean isAny() {
		return any;
	}

	public void setAny(boolean any) {
		this.any = any;
	}
}
