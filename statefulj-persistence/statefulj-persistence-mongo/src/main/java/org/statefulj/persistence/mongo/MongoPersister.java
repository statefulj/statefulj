
package org.statefulj.persistence.mongo;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.bson.types.ObjectId;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.statefulj.common.utils.ReflectionUtils.*;

import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.common.AbstractPersister;
import org.statefulj.persistence.mongo.model.StateDocument;

import com.mongodb.DBObject;

public class MongoPersister<T> 
			extends AbstractPersister<T> 
			implements 
				Persister<T>, 
				BeanDefinitionRegistryPostProcessor, 
				ApplicationListener<ApplicationEvent>, 
				ApplicationContextAware {
	
	public static final String COLLECTION = "managedState";
	
	MongoTemplate mongoTemplate;
	
	ApplicationContext appContext; 
	
	String repoId;
	
	String templateId;

	// Private class
	//
	@Document(collection=COLLECTION)
	class StateDocumentImpl implements StateDocument {

		@Id
		String id;
		
		@Transient
		boolean persisted = true;
		
		String state;
		
		String prevState;
		
		@DBRef(lazy=true)
		Object owner;
		
		Date updated;
		
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

		public void setState(String state) {
			this.state = state;
		}

		@Override
		public String getState() {
			return state;
		}

		public String getPrevState() {
			return prevState;
		}

		public void setPrevState(String prevState) {
			this.prevState = prevState;
		}

		public Object getOwner() {
			return owner;
		}

		public void setOwner(Object owner) {
			this.owner = owner;
		}

		public Date getUpdated() {
			return updated;
		}

		public void setUpdated(Date updated) {
			this.updated = updated;
		}
	}
	
	public MongoPersister(
			List<State<T>> states, 
			String stateFieldName, 
			State<T> start, 
			Class<T> clazz, 
			String repoId) {
		super(states, stateFieldName, start,clazz);
		this.repoId = repoId;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.appContext = applicationContext;
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
			if (stateDoc != null && stateDoc.isPersisted()) {
				
				// Entity is in the database - perform qualified update based off 
				// the current State value
				//
				Query query = buildQuery(stateDoc, current);
				Update update = buildUpdate(current, next);

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
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(
			BeanDefinitionRegistry registry) throws BeansException {
		
		// Fetch the MongoTemplate Bean Id
		//
		BeanDefinition repo = registry.getBeanDefinition(this.repoId);
		this.templateId = ((BeanReference)repo.getPropertyValues().get("mongoOperations")).getBeanName();
		
		// Add in CascadeSupport
		//
		BeanDefinition mongoCascadeSupportBean = BeanDefinitionBuilder
				.genericBeanDefinition(MongoCascadeSupport.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = mongoCascadeSupportBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, this);
		registry.registerBeanDefinition(Long.toString((new Random()).nextLong()), mongoCascadeSupportBean);
	}

	@SuppressWarnings("unchecked")
	/***
	 * Cascade the Save to the StateDocument
	 * 
	 * @param obj
	 * @param dbo
	 */
	public void onAfterSave(Object stateful, DBObject dbo) {
		
		// Is the Class being saved the managed class?
		//
		if (stateful.getClass().equals(getClazz())) {
			try {
				boolean updateStateful = false;
				StateDocumentImpl stateDoc = this.getStateDocument((T)stateful);
				
				// If the StatefulDocument doesn't have an associated StateDocument, then 
				// we need to create a new StateDocument - save the StateDocument and save the
				// Stateful Document again so that they both valid DBRef objects
				//
				if (stateDoc == null) {
					stateDoc = createStateDocument((T)stateful);
					stateDoc.setUpdated(Calendar.getInstance().getTime());
					updateStateful = true;
				}
				if (!stateDoc.isPersisted()) {
					stateDoc.setOwner(stateful);
					this.mongoTemplate.save(stateDoc);
					stateDoc.setPersisted(true);
					if (updateStateful) {
						this.mongoTemplate.save(stateful);
					}
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			
		}
	}
	
	@Override
	/***
	 * After Spring has completed initializing all Beans, fetch the MongoTemplate.  Doing it 
	 * earlier than this callback can lead to errors due to premature initialization
	 */
	public void onApplicationEvent(ApplicationEvent event) {
		if (ContextRefreshedEvent.class.isAssignableFrom(event.getClass())) {
			this.mongoTemplate = (MongoTemplate)appContext.getBean(this.templateId);
		}
	}

	@Override
	protected boolean validStateField(Field stateField) {
		return stateField.getType().equals(StateDocument.class);
	}

	@Override
	protected Field findIdField(Class<?> clazz) {
		return getFirstAnnotatedField(this.getClazz(), Id.class);
	}

	@Override
	protected Class<?> getStateFieldType() {
		return StateDocumentImpl.class;
	}

	protected Query buildQuery(StateDocumentImpl state, State<T> current) {
		return Query.query(new Criteria("_id").is(state.getId()).and("state").is(current.getName()));
	}

	protected Update buildUpdate(State<T> current, State<T> next) {
		Update update = new Update();
		update.set("prevState", current.getName());
		update.set("state", next.getName());
		update.set("updated", Calendar.getInstance().getTime());
		return update;
	}
	
	protected String getState(T stateful) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		StateDocumentImpl stateDoc = this.getStateDocument(stateful);
		return (stateDoc != null) ? stateDoc.getState() : getStart().getName();
	}
	
	protected void setState(T stateful, String state) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		StateDocumentImpl stateDoc = this.getStateDocument(stateful);
		if (stateDoc == null) {
			stateDoc = createStateDocument(stateful);
		}
		stateDoc.setPrevState(stateDoc.getState());
		stateDoc.setState(state);
		stateDoc.setUpdated(Calendar.getInstance().getTime());
	}

	@SuppressWarnings("unchecked")
	protected StateDocumentImpl getStateDocument(T stateful) throws IllegalArgumentException, IllegalAccessException {
		return (StateDocumentImpl)getStateField().get(stateful);
	}
	
	protected StateDocumentImpl createStateDocument(T stateful) throws IllegalArgumentException, IllegalAccessException {
		StateDocumentImpl stateDoc = new StateDocumentImpl();
		stateDoc.setPersisted(false);
		stateDoc.setId(new ObjectId().toHexString());
		stateDoc.setState(getStart().getName());
		setStateDocument(stateful, stateDoc);
		return stateDoc;
	}
	
	protected void setStateDocument(T stateful, StateDocument stateDoc) throws IllegalArgumentException, IllegalAccessException {
		getStateField().set(stateful, stateDoc);
	}
	
	protected void updateInMemory(T stateful, StateDocumentImpl stateDoc, String current, String next) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, StaleStateException {
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

	protected void throwStaleState(String current, String next) throws StaleStateException {
		String err = String.format(
				"Unable to update state, entity.state=%s, db.state=%s",
				current,
				next);
		throw new StaleStateException(err);
	}

}
