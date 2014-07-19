package org.statefulj.fsm;

/**
 * Indicates that the FSM should wait for a period before retrying the Event
 * 
 * @author andrewhall
 *
 */
public class WaitAndRetryException extends RetryException {
	
	private static final long serialVersionUID = 1L;

	private int wait;
	
	public WaitAndRetryException(int wait) {
		this.wait = wait;
	}

	public int getWait() {
		return wait;
	}

	public void setWait(int wait) {
		this.wait = wait;
	}

}
