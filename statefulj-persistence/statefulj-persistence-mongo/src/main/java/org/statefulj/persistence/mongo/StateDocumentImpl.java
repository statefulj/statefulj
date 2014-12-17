/**
 * 
 */
package org.statefulj.persistence.mongo;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.statefulj.persistence.mongo.model.StateDocument;

/**
 * @author Andrew Hall
 *
 */
// Private class
//
@Document(collection=StateDocumentImpl.COLLECTION)
class StateDocumentImpl implements StateDocument {

	public static final String COLLECTION = "managedState";
	
	@Id
	String id;
	
	@Transient
	boolean persisted = true;
	
	String state;
	
	String prevState;
	
	Date updated;
	
	String managedCollection;
	
	Object managedId;
	
	String managedField;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isPersisted() {
		return persisted;
	}

	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}

	@Override
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public String getPrevState() {
		return prevState;
	}

	public void setPrevState(String prevState) {
		this.prevState = prevState;
	}

	@Override
	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	@Override
	public String getManagedCollection() {
		return managedCollection;
	}

	public void setManagedCollection(String managedCollection) {
		this.managedCollection = managedCollection;
	}

	@Override
	public Object getManagedId() {
		return managedId;
	}

	public void setManagedId(Object managedId) {
		this.managedId = managedId;
	}

	@Override
	public String getManagedField() {
		return managedField;
	}

	public void setManagedField(String managedField) {
		this.managedField = managedField;
	}
}


