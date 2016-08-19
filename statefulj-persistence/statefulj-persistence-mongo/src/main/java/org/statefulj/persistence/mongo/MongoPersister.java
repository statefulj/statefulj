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

import com.mongodb.DBObject;
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
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.LazyLoadingProxy;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.common.AbstractPersister;
import org.statefulj.persistence.mongo.model.StateDocument;

import javax.persistence.EmbeddedId;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import static org.statefulj.common.utils.ReflectionUtils.getReferencedField;

public class MongoPersister<T>
			extends AbstractPersister<T, StateDocumentImpl>
			implements
				Persister<T>,
				BeanDefinitionRegistryPostProcessor,
				ApplicationContextAware {

	final static FindAndModifyOptions RETURN_NEW = FindAndModifyOptions.options().returnNew(true);

	private ApplicationContext appContext;

	private String repoId;

	private MongoTemplate mongoTemplate;

	private String templateId;

	/**
	 * Instantiate the MongoPersister with a specified template.  The State field
	 * on the Entity will be determined by inspection of Entity for the @State annotation
	 *
	 * @param states List of the States
	 * @param startState The Start State
	 * @param clazz The Managed Entity class
	 * @param mongoTemplate MongoTemplate to use to persist the Managed Entity
	 */
	public MongoPersister(
			List<State<T>> states,
			State<T> startState,
			Class<T> clazz,
			MongoTemplate mongoTemplate) {
		super(states, null, startState, clazz);
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Instantiate the MongoPersister with a specified template.  The MongoPersister
	 * will use the stateFieldName to determine the State field.
	 *
	 * @param states List of the States
	 * @param stateFieldName The name of the State Field
	 * @param startState The Start State
	 * @param clazz The Managed Entity class
	 * @param mongoTemplate MongoTemplate to use to persist the Managed Entity
	 */
	public MongoPersister(
			List<State<T>> states,
			String stateFieldName,
			State<T> startState,
			Class<T> clazz,
			MongoTemplate mongoTemplate) {
		super(states, stateFieldName, startState, clazz);
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Instantiate the MongoPersister with the id of the MongoRepository bean for
	 * the Managed Entity.  The State field on the Entity will be
	 * determined by inspection of Entity for the @State annotation
	 *
	 * @param states List of the States
	 * @param startState The Start State
	 * @param clazz The Managed Entity class
	 * @param repoId Bean Id of the Managed Entity's MongoRepository
	 */
	public MongoPersister(
			List<State<T>> states,
			State<T> startState,
			Class<T> clazz,
			String repoId) {
		this(states, null, startState,clazz, repoId);
	}

	/**
	 * Instantiate the MongoPersister with the id of the MongoRepository bean for
	 * the Managed Entity.  The MongoPersister will use the stateFieldName to
	 * determine the State field.
	 *
	 * @param states List of States
	 * @param stateFieldName The name of the State Field
	 * @param startState The Start State
	 * @param clazz The Managed Entity class
	 * @param repoId Bean Id of the Managed Entity's MongoRepository
	 */
	public MongoPersister(
			List<State<T>> states,
			String stateFieldName,
			State<T> startState,
			Class<T> clazz,
			String repoId) {
		super(states, stateFieldName, startState, clazz);
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
	 * @param stateful Stateful Entity
	 * @param current Expected current State
	 * @param next The value of the next State
	 * @throws StaleStateException thrown if the value of the State does not equal to the provided current State
	 */
	@Override
	public void setCurrent(T stateful, State<T> current, State<T> next) throws StaleStateException {
		try {

			// Has this Entity been persisted to Mongo?
			//
			StateDocumentImpl stateDoc = this.getStateDocument(stateful);
			if (stateDoc != null && stateDoc.isPersisted()) {

				// Update state in the DB
				//
				updateStateInDB(stateful, current, next, stateDoc);
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

	/**
	 * @param stateful
	 * @param current
	 * @param next
	 * @param stateDoc
	 * @throws IllegalAccessException
	 * @throws StaleStateException
	 */
	private void updateStateInDB(T stateful, State<T> current, State<T> next,
			StateDocumentImpl stateDoc) throws IllegalAccessException,
			StaleStateException {
		// Entity is in the database - perform qualified update based off
		// the current State value
		//
		Query query = buildQuery(stateDoc, current);
		Update update = buildUpdate(current, next);

		// Update state in DB
		//
		StateDocumentImpl updatedDoc = updateStateDoc(query, update);
		if (updatedDoc != null) {

			// Success, update in memory
			//
			setStateDocument(stateful, updatedDoc);

		} else {

			// If we aren't able to update - it's most likely that we are out of sync.
			// So, fetch the latest value and update the Stateful object.  Then throw a RetryException
			// This will cause the event to be reprocessed by the FSM
			//
			updatedDoc = findStateDoc(stateDoc.getId());

			if (updatedDoc != null) {
				String currentState = stateDoc.getState();
				setStateDocument(stateful, updatedDoc);
				throwStaleState(currentState, updatedDoc.getState());
			} else {
				throw new RuntimeException("Unable to find StateDocument with id=" + stateDoc.getId());
			}
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(
			BeanDefinitionRegistry registry) throws BeansException {

		if (this.mongoTemplate == null) {

			if (this.repoId != null) {

				// Fetch the MongoTemplate Bean Id
				//
				BeanDefinition repo = registry.getBeanDefinition(this.repoId);
				this.templateId = ((BeanReference)repo.getPropertyValues().get("mongoOperations")).getBeanName();
			}

			// Check to make sure we have a reference to the MongoTemplate
			//
			if (this.templateId == null) {
				throw new RuntimeException("Unable to obtain a reference to a MongoTemplate");
			}
		}


		// Add in CascadeSupport
		//
		BeanDefinition mongoCascadeSupportBean = BeanDefinitionBuilder
				.genericBeanDefinition(MongoCascadeSupport.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = mongoCascadeSupportBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, this);
		registry.registerBeanDefinition(Long.toString((new Random()).nextLong()), mongoCascadeSupportBean);
	}

	@Override
	protected boolean validStateField(Field stateField) {
		return stateField.getType().equals(StateDocument.class);
	}

	@Override
	protected Field findIdField(Class<?> clazz) {
		Field idField = getReferencedField(this.getClazz(), Id.class);
		if (idField == null) {
			idField = getReferencedField(this.getClazz(), javax.persistence.Id.class);
			if (idField == null) {
				idField = getReferencedField(this.getClazz(), EmbeddedId.class);
			}
		}
		return idField;
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
		return (stateDoc != null) ? stateDoc.getState() : getStartState().getName();
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

	protected StateDocumentImpl getStateDocument(T stateful) throws IllegalArgumentException, IllegalAccessException {
		Object stateDoc = getStateField().get(stateful);
		if (stateDoc instanceof LazyLoadingProxy) {
			stateDoc = ((LazyLoadingProxy)stateDoc).getTarget();
		}
		return (StateDocumentImpl)stateDoc;
	}

	protected StateDocumentImpl createStateDocument(T stateful) throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException {
		StateDocumentImpl stateDoc = new StateDocumentImpl();
		stateDoc.setPersisted(false);
		stateDoc.setId(new ObjectId().toHexString());
		stateDoc.setState(getStartState().getName());
		stateDoc.setManagedCollection(getMongoTemplate().getCollectionName(stateful.getClass()));
		stateDoc.setManagedField(this.getStateField().getName());
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

	protected MongoTemplate getMongoTemplate() {
		if (this.mongoTemplate == null) {
			this.mongoTemplate = (MongoTemplate)appContext.getBean(this.templateId);
		}
		return this.mongoTemplate;
	}

	protected StateDocumentImpl updateStateDoc(Query query, Update update) {
		return (StateDocumentImpl)getMongoTemplate().findAndModify(query, update, RETURN_NEW, StateDocumentImpl.class);
	}

	protected StateDocumentImpl findStateDoc(String id) {
		return (StateDocumentImpl)getMongoTemplate().findById(id, StateDocumentImpl.class);
	}

	@SuppressWarnings("unchecked")
	/***
	 * Cascade the Save to the StateDocument
	 *
	 * @param obj
	 * @param dbo
	 */
	void onAfterSave(Object stateful, DBObject dbo) {

		// Is the Class being saved the managed class?
		//
		if (getClazz().isAssignableFrom(stateful.getClass())) {
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
					stateDoc.setManagedId(this.getId((T)stateful));
					this.getMongoTemplate().save(stateDoc);
					stateDoc.setPersisted(true);
					if (updateStateful) {
						this.getMongoTemplate().save(stateful);
					}
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			}

		}
	}

	void onAfterDelete(Class<?> stateful, DBObject obj) {
		if (stateful != null && getClazz().isAssignableFrom(stateful)) {
			Criteria criteria = new Criteria("managedId").is(obj.get(this.getIdField().getName())).
					and("managedCollection").is(getMongoTemplate().getCollectionName(getClazz())).
					and("managedField").is(this.getStateField().getName());
			this.getMongoTemplate().remove(new Query(criteria), StateDocumentImpl.class);
		}
	}
}

