package org.statefulj.persistence.mongo;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;

import com.mongodb.DBObject;

class MongoCascadeSupport<T> extends AbstractMongoEventListener<Object> {

	MongoPersister<T> persister;
	
	public MongoCascadeSupport(MongoPersister<T> persister) {
		this.persister = persister;
	}
	
	/**
	 * Pass the Save event to the MongoPersister to cascade to the StateDocument
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener#onBeforeConvert(java.lang.Object)
	 */
	@Override
	public void onAfterSave(Object source, DBObject dbo) {
		this.persister.onAfterSave(source, dbo);
	}
}
