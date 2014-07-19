package org.statefulj.fsm.model;

public interface Transition<T> {

	StateActionPair<T> getStateActionPair();
}
