package org.statefulj.fsm;

/**
 * Exception indicates that the FSM needs to retry the event
 * 
 * @author andrewhall
 *
 */
public class RetryException extends Exception {

	private static final long serialVersionUID = 1L;

	public RetryException() {
		super();
	}
	
	public RetryException(String msg) {
		super(msg);
	}
}
