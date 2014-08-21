package org.statefulj.fsm;

import org.statefulj.fsm.model.State;

/**
 * A Persister is responsible for maintaining the persistence of the current State of the
 * FSM
 * 
 * @author Andrew Hall
 *
 */
public interface Persister<T> {
	
	/**
	 * Returns the currently Persisted state
	 * 
	 * @param id of the Object
	 * @return current State
	 */
	State<T> getCurrent(T obj);

	/**
	 * Set the current state to the next state.  Will serialize access to the persistence
	 * and ensure that expected current state is indeed the current state.  If not, will throw
	 * a StaleStateException
	 * 
	 * @param id of the Object
	 * @param current
	 * @param next
	 * @throws StaleStateException
	 */
	void setCurrent(T obj, State<T> current, State<T> next) throws StaleStateException;
}
