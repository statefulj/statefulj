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
package org.statefulj.persistence.mongo;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.statefulj.persistence.mongo.model.StateDocument;

@Document(collection=StateDocumentImpl.COLLECTION)
class StateDocumentImpl implements StateDocument {

	public static final String COLLECTION = "managedState";

	@Id
	private String id;

	@Transient
	private boolean persisted = true;

	private String state;

	private String prevState;

	private Date updated;

	private String managedCollection;

	private Object managedId;

	private String managedField;

	@Override
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


