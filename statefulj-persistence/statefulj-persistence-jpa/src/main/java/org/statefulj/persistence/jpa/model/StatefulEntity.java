package org.statefulj.persistence.jpa.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.statefulj.persistence.common.annotations.State;

/**
 * A convenience class for a Stateful Entity.  This Class ensures read-only behavior for the State
 * field
 * 
 * @author Andrew Hall
 *
 */
@MappedSuperclass
public abstract class StatefulEntity {
	
	/**
	 * Field be set on insert.  But updates must be done through the JPAPersister
	 * 
	 */
	@State
	@Column(insertable=true, updatable=false)
	private String state;
	
	public String getState() {
		return state;
	}


}
