package org.statefulj.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.StateActionPair;
import org.statefulj.fsm.model.Transition;

/**
 * The FSM is responsible for the processing the event with the current State and persisting
 * the State with the composite Persister
 * 
 * @author andrewhall
 *
 */
public class FSM<T> {
	
	Logger logger = LoggerFactory.getLogger(FSM.class);

	static final int DEFAULT_RETRIES = 20;

	private int retries;
	private Persister<T> persister;
	private String name = "FSM";
	
	/**
	 * 
	 * @param persister
	 */
	public FSM(Persister<T> persister) {
		this.persister = persister;
		this.retries = DEFAULT_RETRIES;
	}
	
	/**
	 * 
	 * @param persister
	 */
	public FSM(String name, Persister<T> persister) {
		this.name = name;
		this.persister = persister;
		this.retries = DEFAULT_RETRIES;
	}
	
	/**
	 * 
	 * @param persister
	 * @param retries
	 */
	public FSM(Persister<T> persister, int retries) {
		this.persister = persister;
		this.retries = retries;
	}

	/**
	 * Process event.  Will handle all retry attempts.  If attempts exceed maximum retries,
	 * it will throw a TooBusyException.  
	 * 
	 * @param stateful
	 * @param event
	 * @param args
	 * @return
	 * @throws TooBusyException
	 */
	public State<T> onEvent(final T stateful, final String event, final Object ... args) throws TooBusyException {
		
		int attempts = 0;
		
		while(attempts < this.retries) {
			try {
				State<T> current = persister.getCurrent(stateful);
				
				// Fetch the transition for this event from the current state
				//
				Transition<T> transition = current.getTransition(event);
				
				// Is there one?
				//
				if (transition != null) {
					StateActionPair<T> pair = transition.getStateActionPair();
					persister.setCurrent(stateful, current, pair.getState());
					Action<T> action = pair.getAction();
					
					logger.debug("{}({})::{}/{} -> {}/{}", 
							this.name,
							stateful,
							current.getName(), 
							event, 
							pair.getState().getName(), 
							(action == null) ? "noop" : action.getClass().getSimpleName());
					
					if (action != null) {
						pair.getAction().execute(stateful, event, args);
					}
					current = pair.getState();
				} else {
					logger.debug("{}({})::{}/{} -> {}/noop", 
							this.name, 
							stateful,
							current.getName(), 
							event,
							current.getName());
				}
				return current;
			} catch(RetryException re) {
				
				logger.warn("{}({})::Retrying event", this.name, stateful);
				
				// Wait?
				//
				if (WaitAndRetryException.class.isInstance(re)) {
					try {
						Thread.sleep(((WaitAndRetryException)re).getWait());
					} catch(InterruptedException ie) {
						throw new RuntimeException(ie);
					}
				}
				attempts++;
			}
		}
		logger.error("{}({})::Unable to process event", this.name, stateful);
		throw new TooBusyException();
	}
	
	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public Persister<T> getPersister() {
		return persister;
	}

	public void setPersister(Persister<T> persister) {
		this.persister = persister;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public State<T> getCurrentState(T obj) {
		return this.persister.getCurrent(obj);
	}
}
