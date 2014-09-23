package org.statefulj.persistence.mongo;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;

import com.mongodb.DBObject;

class MongoCascadeSupport<T> extends AbstractMongoEventListener<Object> {

	MongoPersister<T> persister;
	
	public MongoCascadeSupport(MongoPersister<T> persister) {
		this.persister = persister;
	}
	
	/**
	 * If this is the first time the Entity is being saved to Mongo - then 
	 * we need to cascade the save to the StateDocument.  If one doesn't exist,
	 * create one and set to initial state
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener#onBeforeConvert(java.lang.Object)
	 */
	@Override
	public void onBeforeConvert(final Object obj) {
		this.persister.onBeforeConvert(obj);
	}	

	@Override
	public void onAfterSave(Object source, DBObject dbo) {
		this.persister.onAfterSave(source, dbo);
	}
}
