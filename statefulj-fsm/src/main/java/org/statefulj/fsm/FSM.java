/***
 *
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
 * @author Andrew Hall
 *
 */
public class FSM<T> {

	private static final Logger logger = LoggerFactory.getLogger(FSM.class);

	private static final int DEFAULT_RETRIES = 20;
	private static final int DEFAULT_RETRY_INTERVAL = 250;  // 250 ms

	private int retryAttempts = DEFAULT_RETRIES;
	private int retryInterval = DEFAULT_RETRY_INTERVAL;

	private Persister<T> persister;
	private String name = "FSM";

	/**
	 * FSM Constructor with the name of the FSM
	 *
	 * @param name Name associated with the FSM
	 */
	public FSM(String name) {
		this.name = name;
	}

	/**
	 * FSM Constructor with the Persister responsible for setting the State on the Entity
	 *
	 * @param persister Persister responsible for setting the State on the Entity
	 */
	public FSM(Persister<T> persister) {
		this.persister = persister;
	}

	/**
	 * FSM Constructor with the name of the FSM and Persister responsible for setting the State on the Entity
	 *
	 * @param name Name associated with the FSM
	 * @param persister Persister responsible for setting the State on the Entity
	 */
	public FSM(String name, Persister<T> persister) {
		this.name = name;
		this.persister = persister;
	}

	/**
	 * FSM Constructor
	 *
	 * @param name Name associated with the FSM
	 * @param persister Persister responsible for setting the State on the Entity
	 * @param retryAttempts Number of Retry Attempts.  A value of -1 indicates unlimited Attempts
	 * @param retryInterval Time between Retry Attempts in milliseconds
	 */
	public FSM(String name, Persister<T> persister, int retryAttempts, int retryInterval) {
		this.name = name;
		this.persister = persister;
		this.retryAttempts = retryAttempts;
		this.retryInterval = retryInterval;
	}

	/**
	 * Process event.  Will handle all retry attempts.  If attempts exceed maximum retries,
	 * it will throw a TooBusyException.
	 *
	 * @param stateful The Stateful Entity
	 * @param event The Event
	 * @param args Optional parameters to pass into the Action
	 * @return The current State
	 * @throws TooBusyException Exception indicating that we've exceeded the number of RetryAttempts
	 */
	public State<T> onEvent(T stateful, String event, Object ... args) throws TooBusyException {

		int attempts = 0;

		while(this.retryAttempts == -1 || attempts < this.retryAttempts) {
			try {
				State<T> current = this.getCurrentState(stateful);

				// Fetch the transition for this event from the current state
				//
				Transition<T> transition = this.getTransition(event, current);

				// Is there one?
				//
				if (transition != null) {
					current = this.transition(stateful, current, event, transition, args);
				} else {

                    if (logger.isDebugEnabled())
                        logger.debug("{}({})::{}({})->{}/noop",
                                this.name,
                                stateful.getClass().getSimpleName(),
                                current.getName(),
                                event,
                                current.getName());

					// If blocking, force a transition to the current state as
					// it's possible that another thread has moved out of the blocking state.
					// Either way, we'll retry this event
					//
					if (current.isBlocking()) {
						this.setCurrent(stateful, current, current);
						throw new WaitAndRetryException(this.retryInterval);
					}
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

	public int getRetryAttempts() {
		return retryAttempts;
	}

	public void setRetryAttempts(int retries) {
		this.retryAttempts = retries;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
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

	protected Transition<T> getTransition(String event, State<T> current) {
		return current.getTransition(event);
	}

	protected State<T> transition(T stateful, State<T> current, String event, Transition<T> transition, Object... args) throws RetryException {
		StateActionPair<T> pair = transition.getStateActionPair(stateful);
		setCurrent(stateful, current, pair.getState());
		executeAction(
				pair.getAction(),
				stateful,
				event,
				current.getName(),
				pair.getState().getName(),
				args);
		return pair.getState();
	}

	protected void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		persister.setCurrent(stateful, current, next);
	}

	protected void executeAction(
			Action<T> action,
			T stateful,
			String event,
			String from,
			String to,
			Object... args) throws RetryException {

        if (logger.isDebugEnabled())
            logger.debug("{}({})::{}({})->{}/{}",
                    this.name,
                    stateful.getClass().getSimpleName(),
                    from,
                    event,
                    to,
                    (action == null) ? "noop" : action.toString());

		if (action != null) {
			action.execute(stateful, event, args);
		}
	}
}
