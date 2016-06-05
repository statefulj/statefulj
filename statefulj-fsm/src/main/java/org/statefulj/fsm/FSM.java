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
import org.statefulj.fsm.model.impl.DeterministicTransitionImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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


	/**
	 * Fluid FSM builder
	 *
	 * @author Andrew Hall
	 *
	 * @param <T> Type of the Stateful Entity
	 */
	public static class FSMBuilder<T> {

		/**
		 * Fluid State Builder
		 * 
		 * @author Andrew Hall
		 *
		 * @param <T> Type of the Stateful Entity
		 */
		public static class StateBuilder<T> {

			/**
			 * Fluid Transition Builder
			 * 
			 * @author Andrew Hall
			 *
			 * @param <T> Type of the Stateful Entity
			 */
			public static class TransitionBuilder<T> {

				private StateBuilder<T> stateBuilder;
				private String event;
				private String toStateName;
				private State<T> toState;
				private Action<T> action;

				private TransitionBuilder(String event, StateBuilder<T> stateBuilder) {
					this.event = event;
					this.stateBuilder = stateBuilder;
					this.toStateName = null;
					this.action = null;
				}

				private TransitionBuilder(String event, String toStateKey) {
					this(event, toStateKey, null);
				}

				private TransitionBuilder(String event, String toStateKey, Action<T> action) {
					if (event == null || event.trim().equals("")) {
						throw new RuntimeException("You must provide an Event");
					}
					this.event = event;
					this.toStateName = toStateKey;
					this.toState = null;
					this.action = action;
				}

				private TransitionBuilder(String event, State<T> toState, Action<T> action) {
					if (event == null || event.trim().equals("")) {
						throw new RuntimeException("You must provide an Event");
					}
					this.event = event;
					this.toState = toState;
					this.action = action;
				}

				/**
				 * Set the "To" State using the specified State Name
				 * @param toStateName Name of the State
				 * @return TransitionBuilder
				 */
				public TransitionBuilder<T> setToState(String toStateName) {
					this.toStateName = toStateName;
					return this;
				}

				/**
				 * Set the "To" State to the specified State.  If both the "To" State is
				 * set using the name of the State and the State object, the State object 
				 * will take precedence
				 * @param toState To State object
				 * @return TransitionBuilder
				 */
				public TransitionBuilder<T> setToState(State<T> toState) {
					this.toState = toState;
					return this;
				}

				/**
				 * Set the Action for this Transition
				 *
				 * @param action
				 * @return TransitionBuilder
				 */
				public TransitionBuilder<T> setAction(Action<T> action) {
					this.action = action;
					return this;
				}

				/**
				 * Completes the setup for this Transition Builder
				 *
				 * @return StateBuilder
				 */
				public StateBuilder<T> done() {
					return this.stateBuilder;
				}

				private Transition<T> build(State<T> from, Map<String, State<T>> states) {
					State<T> to = (this.toState != null)
							? this.toState
							: (this.toStateName != null)
								? states.get(this.toStateName)
								: from;
					return new DeterministicTransitionImpl<T>(from, to, this.event, this.action);
				}

			}

			private String stateName;
			private Map<String, Transition<T>> transistions = new HashMap<String, Transition<T>>();
			private List<TransitionBuilder<T>> transistionBuilders = new LinkedList<TransitionBuilder<T>>();
			private boolean isEndState = false;
			private boolean isBlocking = false;
			private State<T> state;

			private FSMBuilder<T> fsmBuilder;

			private StateBuilder(FSMBuilder<T> fsmBuilder, String stateName) {
				if (stateName == null || stateName.trim().equals("")) {
					throw new RuntimeException("You must provide a State name");
				}
				this.fsmBuilder = fsmBuilder;
				this.stateName = stateName;
			}

			/**
			 * Set whether this State is the End State
			 * @param isEndState
			 * @return StateBuilder
			 */
			public StateBuilder<T> setEndState(boolean isEndState) {
				this.isEndState = isEndState;
				return this;
			}

			/**
			 * Set whether this State is a blocking State
			 * @param isBlocking
			 * @return StateBuilder
			 */
			public StateBuilder<T> setBlockingState(boolean isBlocking) {
				this.isBlocking = isBlocking;
				return this;
			}

			/**
			 * Add a Transition with no Action.
			 * @param event Event that triggers the Transition
			 * @param toState The name of the State in which to transition to
			 * @return StateBuilder
			 */
			public StateBuilder<T> addTransition(String event, String toState) {
				this.transistionBuilders.add(new TransitionBuilder<T>(event, toState));
				return this;
			}

			/**
			 * Add a Transition with no Action.
			 * @param event Event that triggers the Transition
			 * @param toState The State in which to transition to
			 * @return StateBuilder
			 */
			public StateBuilder<T> addTransition(String event, State<T> toState) {
				return this.addTransition(event, toState, null);
			}

			/**
			 * Add a Transition with an Action
			 * @param event Event that triggers the Transition
			 * @param toState The name of the State in which to transition to
			 * @param action The Action to invoke
			 * @return StateBuilder
			 */
			public StateBuilder<T> addTransition(String event, String toState, Action<T> action) {
				this.transistionBuilders.add(new TransitionBuilder<T>(event, toState, action));
				return this;
			}

			/**
			 * Add a Transition with an Action
			 * @param event Event that triggers the Transition
			 * @param toState The State in which to transition to
			 * @param action The Action to invoke
			 * @return StateBuilder
			 */
			public StateBuilder<T> addTransition(String event, State<T> toState, Action<T> action) {
				this.fsmBuilder.addState(toState);
				this.transistionBuilders.add(new TransitionBuilder<T>(event, toState, action));
				return this;
			}

			/**
			 * Add a Transition with no State change but with an Action
			 * @param event Event that triggers the Transition
			 * @param action The Action to invoke
			 * @return StateBuilder
			 */
			public StateBuilder<T> addTransition(String event, Action<T> action) {
				return this.addTransition(event, this.stateName, action);
			}

			/**
			 * Add an instantiated Transition
			 * @param event Event that triggers the Transition
			 * @return StateBuilder
			 */
			public StateBuilder<T> addTransition(String event, Transition<T> transition) {
				this.transistions.put(event, transition);
				return this;
			}

			/**
			 * Begin building a Transition
			 * @param event Event that triggers the Transition
			 * @return StateBuilder
			 */
			public TransitionBuilder<T> buildTransition(String event) {
				TransitionBuilder<T> transitionBuilder = new TransitionBuilder<T>(event, this);
				this.transistionBuilders.add(transitionBuilder);
				return transitionBuilder;
			}

			/**
			 * Completes the setup for this State Builder
			 * @return FSMBuilder
			 */
			public FSMBuilder<T> done() {
				return this.fsmBuilder;
			}

			private State<T> buildState() {
				this.state = new StateImpl<T>(this.stateName, this.transistions, this.isEndState, this.isBlocking);
				return this.state;
			}

			private void buildTransitions(Map<String, State<T>> states) {
				for(TransitionBuilder<T> transitionBuilder : this.transistionBuilders) {
					transitionBuilder.build(this.state, states);
				}
			}

		}

		private int retryAttempts = DEFAULT_RETRIES;
		private int retryInterval = DEFAULT_RETRY_INTERVAL;

		private Persister<T> persister;
		private String name = "FSM";

		private HashMap<String, State<T>> states = new HashMap<String, State<T>>();
		private List<StateBuilder<T>> stateBuilders = new LinkedList<StateBuilder<T>>();
		private String startState;

		/**
		 * Build a new FSM Builder
		 * @param clazz Parameterized Class for the FSM
		 * @return FSMBuilder
		 */
		public static <T> FSMBuilder<T> newBuilder(Class<T> clazz) {
			return new FSMBuilder<T>();
		}

		/**
		 * Set the Persister for the FSM.  If not specified, a MemoryPersister will be
		 * instantiated with persists the state only on the Stateful object
		 * @param persister The Persister
		 * @return FSMBuilder
		 */
		public FSMBuilder<T> setPerister(Persister<T> persister) {
			this.persister = persister;
			return this;
		}

		/**
		 * Sets the name of the FSM
		 * @param name Name of the FSM
		 * @return FSMBuilder
		 */
		public FSMBuilder<T> setName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the number of retry attempts for the FSM
		 * @param retryAttempts
		 * @return FSMBuilder
		 */
		public FSMBuilder<T> setRetryAttempts(int retryAttempts) {
			this.retryAttempts = retryAttempts;
			return this;
		}

		/**
		 * Sets the retry interval for the FSM
		 * @param retryInterval
		 * @return FSMBuilder
		 */
		public FSMBuilder<T> setRetryInterval(int retryInterval) {
			this.retryInterval = retryInterval;
			return this;
		}

		/**
		 * Add a specified State
		 * @param state State to add
		 * @return FSMBuilder
		 */
		public FSMBuilder<T> addState(State<T> state) {
			return this.addState(state, false);
		}

		/**
		 * Add a specified State and state whether or not it's the Start State for the FSM.
		 * If no State is explicitly set as the Start State, then the first State added will
		 * be the Start State
		 * 
		 * @param state State to add
		 * @param isStartState Whether or not the State is specified as the Start State
		 * @return FSMBuilder
		 */
		public FSMBuilder<T> addState(State<T> state, boolean isStartState) {
			this.startState = (this.startState == null || isStartState) ? state.getName() : this.startState;
			this.states.put(state.getName(), state);
			return this;
		}

		/**
		 * Begin building a State for the FSM
		 *
		 * @param name Name of the State
		 * @return StateBuilder
		 */
		public StateBuilder<T> buildState(String name) {
			return this.buildState(name, false);
		}

		/**
		 * Begin building a State for the FSM and state whether or not it's the Start State for the FSM.
		 * If no State is explicitly set as the Start State, then the first State added will
		 * be the Start State
		 * 
		 * @param name Name of the State
		 * @param isStartState Whether or not the State is specified as the Start State
		 * @return StateBuilder
		 */
		public StateBuilder<T> buildState(String name, boolean isStartState) {
			this.startState = (this.startState == null || isStartState) ? name : this.startState;
			StateBuilder<T> stateBuilder = new StateBuilder<T>(this, name);
			this.stateBuilders.add(stateBuilder);
			return stateBuilder;
		}

		/**
		 * Build the FSM and all of it's States and Transitions
		 * @return FSM
		 */
		public FSM<T> build() {

			for(StateBuilder<T> stateBuilder : this.stateBuilders) {
				State<T> state = stateBuilder.buildState();
				this.states.put(state.getName(), state);
			}

			for(StateBuilder<T> stateBuilder : this.stateBuilders) {
				stateBuilder.buildTransitions(this.states);
			}

			State<T> startState = this.states.get(this.startState);
			if (startState == null) {
				throw new RuntimeException("No start state defined, state=" + this.startState);
			}

			if (this.persister == null) {
				this.persister = new MemoryPersisterImpl<T>();
			}
			this.persister.setStates(this.states.values());
			this.persister.setStartState(startState);

			return new FSM<T>(this.name, this.persister, this.retryAttempts, this.retryInterval);
		}
	}


	protected Transition<T> getTransition(String event, State<T> current) {
		return current.getTransition(event);
	}

	protected State<T> transition(T stateful, State<T> current, String event, Transition<T> transition, Object... args) throws RetryException {
		StateActionPair<T> pair = transition.getStateActionPair(stateful, event, args);
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
