package org.statefulj.persistence.mongo.model;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public interface StateDocument {
	
	String getState();
	
	String getPrevState();
	
	Date getUpdated();
	
	Object getOwner();

}
