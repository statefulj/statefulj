
package org.statefulj.persistence.mongo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.mongo.model.StateDocument;

// TODO : Rewrite this to use "safe" query building instead of string construction
//
public class MongoPersister<T> extends AbstractMongoEventListener<Object> implements Persister<T> {
	
	public static final String COLLECTION = "managedState";
	
	@Resource
	MongoTemplate mongoTemplate;

	private Field idField;
	private Field stateField;
	private State<T> start;
	private Class<T> clazz;
	private HashMap<String, State<T>> states = new HashMap<String, State<T>>();
	
	
	// Private class
	//
	@Document(collection=COLLECTION)
	class StateDocumentImpl implements StateDocument {

		@Id
		String id;
		
		String state;
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setState(String state) {
			this.state = state;
		}

		@Override
		public String getState() {
			return state;
		}

	}
	
	public MongoPersister(List<State<T>> states, State<T> start, Class<T> clazz) {
		
		this.clazz = clazz;
		
		// Find the Id and State<T> field of the Entity
		//
		this.idField = getAnnotatedField(clazz, Id.class);
		
		if (this.idField == null) {
			throw new RuntimeException("No Id field defined");
		}
		this.idField.setAccessible(true);
		
		this.stateField = getAnnotatedField(clazz, org.statefulj.persistence.mongo.annotations.State.class);
		if (this.stateField == null) {
			throw new RuntimeException("No State field defined");
		}
		if (!this.stateField.getType().equals(StateDocument.class)) {
			throw new RuntimeException(
					String.format(
							"State field, %s, of class %s, is not of type %s",
							this.stateField.getName(),
							clazz,
							StateDocument.class.getName()));
		}
		this.stateField.setAccessible(true);

		// Start state - returned when no state is set
		//
		this.start = start;
		
		// Index States into a HashMap
		//
		for(State<T> state : states) {
			this.states.put(state.getName(), state);
		}
	}

	/**
	 * Return the current State 
	 */
	public State<T> getCurrent(T stateful) {
		State<T> state = null;
		StateDocumentImpl stateDoc;
		try {
			stateDoc = this.getStateDocument(stateful);
			state = (stateDoc == null) ? this.start : this.states.get(stateDoc.getState());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return state;
	}

	/**
	 * Set the current State.  This method will ensure that the state in the db matches the expected current state.  
	 * If not, it will throw a StateStateException
	 * 
	 * @param stateful
	 * @param current
	 * @param next
	 * @throws StaleStateException 
	 */
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		try {
			
			// Has this Entity been persisted to Mongo? 
			//
			StateDocumentImpl stateDoc = this.getStateDocument(stateful);
			if (stateDoc != null && stateDoc.getId() != null) {
				
				// Entity is in the database - perform qualified update based off 
				// the current State value
				//
				Query query = buildQuery(stateDoc, current);
				Update update = buildUpdate(next);

				// Update state in DB
				//
				StateDocument updatedDoc = mongoTemplate.findAndModify(query, update,  StateDocumentImpl.class);
				if (updatedDoc != null) {
					
					// Success update in memory
					//
					stateDoc.setState(next.getName());
					
				} else {
					
					// If we aren't able to update - it's most likely that we are out of sync.
					// So, fetch the latest value and update the Stateful object.  Then throw a RetryException
					// This will cause the event to be reprocessed by the FSM
					//
					updatedDoc = (StateDocument)mongoTemplate.findById(stateDoc.getId(), StateDocumentImpl.class);
					if (updatedDoc != null) {
						String currentState = stateDoc.getState();
						stateDoc.setState(updatedDoc.getState());
						throwStaleState(currentState, updatedDoc.getState());
					} else {
						throw new RuntimeException("Unable to find StateDocument with id=" + stateDoc.getId());
					}
				}
			} else {
				
				// The Entity hasn't been persisted to Mongo - so it exists only
				// this Application memory.  So, serialize the qualified update to prevent
				// concurrency conflicts
				//
				updateInMemory(stateful, stateDoc, current.getName(), next.getName());
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Query buildQuery(StateDocumentImpl state, State<T> current) {
		return Query.query(new Criteria("_id").is(state.getId()).and("state").is(current.getName()));
	}

	protected Update buildUpdate(State<T> next) {
		Update update = new Update();
		update.set("state", next.getName());
		return update;
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
	@SuppressWarnings("unchecked")
	public void onBeforeConvert(final Object obj) {
		if (obj.getClass().equals(this.clazz)) {
			try {
				StateDocumentImpl stateDoc = this.getStateDocument((T)obj);
				if (stateDoc == null) {
					stateDoc = createStateDocument((T)obj);
				}
				if (stateDoc.getId() == null) {
					this.mongoTemplate.save(stateDoc);
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			
		}
	}
	  
	private void updateInMemory(T stateful, StateDocumentImpl stateDoc, String current, String next) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, StaleStateException {
		synchronized(stateful) {
			if (stateDoc == null) {
				stateDoc = createStateDocument(stateful);
			}
			if (stateDoc.getState().equals(current)) {
				setState(stateful, next);
			} else {
				throwStaleState(current, next);
			}
		}
	}

	/**
	 * Return a field by an Annotation
	 * 
	 * @param clazz
	 * @param annotationClass
	 * @return
	 */
	private Field getAnnotatedField(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		Field match = null;
		if (clazz != null) {
			match = getAnnotatedField(clazz.getSuperclass(), annotationClass);
			if (match == null) {
				for(Field field : clazz.getDeclaredFields()) {
					if (field.isAnnotationPresent(annotationClass)) {
						match = field;
						break;
					}
				}
				
			}
		}
		
		return match;
	}
	
	private Object getId(T stateful) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		return this.idField.get(stateful);
	}

	@SuppressWarnings("unchecked")
	private StateDocumentImpl getStateDocument(T stateful) throws IllegalArgumentException, IllegalAccessException {
		return (StateDocumentImpl)this.stateField.get(stateful);
	}
	
	private StateDocumentImpl createStateDocument(T stateful) throws IllegalArgumentException, IllegalAccessException {
		StateDocumentImpl stateDoc = new StateDocumentImpl();
		stateDoc.setState(this.start.getName());
		setStateDocument(stateful, stateDoc);
		return stateDoc;
	}
	
	private void setStateDocument(T stateful, StateDocument stateDoc) throws IllegalArgumentException, IllegalAccessException {
		this.stateField.set(stateful, stateDoc);
	}
	
	private void setState(T stateful, String state) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		StateDocumentImpl stateDoc = this.getStateDocument(stateful);
		if (stateDoc == null) {
			throw new RuntimeException("Unable to locate the State Document for entity, id=" + getId(stateful));
		}
		stateDoc.setState(state);
	}

	private void throwStaleState(String current, String next) throws StaleStateException {
		String err = String.format(
				"Unable to update state, entity.state=%s, db.state=%s",
				current,
				next);
		throw new StaleStateException(err);
	}
}
