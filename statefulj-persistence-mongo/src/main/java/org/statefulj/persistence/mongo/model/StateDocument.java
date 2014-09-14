package org.statefulj.persistence.mongo.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public interface StateDocument {
	
	String getState();

}
