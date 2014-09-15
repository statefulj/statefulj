package org.statefulj.persistence.mongo.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.statefulj.persistence.common.annotations.State;

/**
 * A convenience class for a Stateful Entity.  This Class ensures read-only behavior for the State
 * field
 * 
 * @author Andrew Hall
 *
 */
public abstract class StatefulDocument {
	
	/**
	 * State field is managed by StatefulJ and is done automically.  So we'll mark this as Transient
	 * to prevent SpringData from overwriting it
	 * 
	 */
	@State
	@DBRef
	private StateDocument state;
	
	public StateDocument getStateDocument() {
		return state;
	}
}
