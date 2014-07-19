package org.statefulj.fsm.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.Transition;

public class StateImpl<T> implements State<T> {

	private String name;
	private Map<String, Transition<T>> transitions = new HashMap<String, Transition<T>>();
	boolean isEndState = false;
	
	public StateImpl() {
	}

	public StateImpl(String name) {
		this.name = name;
	}

	public StateImpl(String name, boolean isEndState) {
		this.name = name;
		this.isEndState = isEndState;
	}

	public StateImpl(String name, Map<String, Transition<T>> transitions, boolean isEndState) {
		this.name = name;
		this.isEndState = isEndState;
		this.transitions = transitions;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Transition<T> getTransition(String event) {
		return transitions.get(event);
	}

	public Map<String, Transition<T>> getTransitions() {
		return transitions;
	}

	public void setTransitions(Map<String, Transition<T>> transitions) {
		this.transitions = transitions;
	}

	public boolean isEndState() {
		return isEndState;
	}

	public void setEndState(boolean isEndState) {
		this.isEndState = isEndState;
	}

	public void addTransition(String event, Transition<T> transition) {
		this.transitions.put(event, transition);
	}
	
	public void removeTransition(String event) {
		this.transitions.remove(event);
	}
}
