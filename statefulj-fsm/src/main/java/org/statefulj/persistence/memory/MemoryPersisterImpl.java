package org.statefulj.persistence.memory;

import java.util.HashMap;

import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;

/**
 * Thread safe, in memory Persister.  
 * 
 * @author andrewhall
 *
 */
public class MemoryPersisterImpl<T> implements Persister<T> {
	
	private HashMap<T, State<T>> states = new HashMap<T, State<T>>();

	public MemoryPersisterImpl() {
	}
	
	public MemoryPersisterImpl(T obj, State<T> start) {
		this.setCurrent(obj, start);
	}
	
	public State<T> getCurrent(T obj) {
		return states.get(obj);
	}
	
	public synchronized void setCurrent(T obj, State<T> current) {
		this.states.put(obj, current);
	}

	/*
	 * Serialize all update of state.  Ensure that the current state is the same State that 
	 * was evaluated. If not, throw an exception
	 * 
	 * (non-Javadoc)
	 * @see org.fsm.Persister#setCurrent(org.fsm.model.State, org.fsm.model.State)
	 */
	public synchronized void setCurrent(T obj, State<T> current, State<T> next) throws StaleStateException {
		if (this.getCurrent(obj).equals(current)) {
			this.setCurrent(obj, next);
		} else {
			throw new StaleStateException();
		}
	}

}
