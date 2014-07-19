package org.statefulj.persistence.jpa.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.statefulj.persistence.jpa.annotations.State;

/**
 * A convenience class for a Stateful Entity.  This Class ensures read-only behavior for the State
 * field
 * 
 * @author andrewhall
 *
 */
@MappedSuperclass
public abstract class StatefulEntity {
	
	/**
	 * Read only field - this field is updated directly by the JPAPersister
	 */
	@State
	@Column(updatable=false)
	private String state;
	
	public String getState() {
		return state;
	}


}
