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
package org.statefulj.framework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.framework.core.actions.DomainEntityMethodInvocationAction;
import org.statefulj.framework.core.actions.MethodInvocationAction;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.fsm.FSM;
import org.statefulj.framework.core.fsm.TransitionImpl;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.core.model.impl.StatefulFSMImpl;
import org.statefulj.fsm.model.impl.StateImpl;

/**
 * StatefulFactory is responsible for inspecting all StatefulControllers and building out
 * the StatefulJ framework.  The factory is invoked at post processing of the beans but before
 * the beans are instantiated
 * 
 * @author Andrew Hall
 *
 */
public class StatefulFactory implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
	
	private ApplicationContext appContext;
	
	private static final String DEFAULT_PACKAGE = "org.statefulj";
	
	private static final Logger logger = LoggerFactory.getLogger(StatefulFactory.class);
	
	private final Pattern binder = Pattern.compile("(([^:]*):)?(.*)");

	private Map<Class<?>, Set<String>> entityToControllerMappings = new HashMap<Class<?>, Set<String>>();
	
	
	private String[] packages;
	
	public StatefulFactory() {
		this(DEFAULT_PACKAGE);
	}
	
	public StatefulFactory(String... packages) {
		this.packages = packages;
	}
	
	// Resolver that injects the FSM for a given controller.  It is inferred by the ClassType or will use the bean Id specified by the value of the 
	// FSM Annotation
	//
	class FSMAnnotationResolver extends QualifierAnnotationAutowireCandidateResolver {
		
		@Override
		public Object getSuggestedValue(DependencyDescriptor descriptor) {
			Object suggested = null;
			Field field = descriptor.getField();
			
			// If this field is annotated with StatefulFSM, determine the bean Id
			//
			if (isAnnotatedWithStatefulFSM(field)) {
				
				// Is the Annotation parameterized with the StatefulController class?
				//
				String controllerId = getControllerIdFromParameterizedValue(field);
				
				// Not parameterized, derive from the generic field type
				//
				if (StringUtils.isEmpty(controllerId)) {
	
					// Get the Managed Class
					//
					Class<?> managedClass = getManagedClass(field);
					
					// Fetch the Controller from the mapping
					//
					controllerId = deriveControllerId(field, managedClass);
				}
				ReferenceFactory refFactory = new ReferenceFactoryImpl(controllerId);
				suggested = appContext.getBean(refFactory.getStatefulFSMId());
			} 
			return (suggested != null) ? suggested : super.getSuggestedValue(descriptor);
		}

		/**
		 * @param field
		 * @return
		 */
		private String getControllerIdFromParameterizedValue(Field field) {
			org.statefulj.framework.core.annotations.FSM fsmAnnotation = field.getAnnotation(org.statefulj.framework.core.annotations.FSM.class);
			String controllerId = (fsmAnnotation != null ) ? fsmAnnotation.value() : null;
			return controllerId;
		}

		/**
		 * @param field
		 * @return
		 */
		private boolean isAnnotatedWithStatefulFSM(Field field) {
			return field != null && field.getType().isAssignableFrom(StatefulFSM.class);
		}

		/**
		 * @param field
		 * @param managedClass
		 * @return
		 */
		private String deriveControllerId(Field field, Class<?> managedClass) {
			String controllerId;
			Set<String> controllers = entityToControllerMappings.get(managedClass);
			
			if (controllers == null) {
				throw new RuntimeException("Unable to resolve FSM for field " + field.getName());
			}
			if (controllers.size() > 1) {
				throw new RuntimeException("Ambiguous fsm for " + field.getName());
			}
			
			controllerId = controllers.iterator().next();
			return controllerId;
		}

		/**
		 * @param field
		 * @return
		 */
		private Class<?> getManagedClass(Field field) {
			Type genericFieldType = field.getGenericType();
			Class<?> managedClass = null;

			if(genericFieldType instanceof ParameterizedType){
			    ParameterizedType aType = (ParameterizedType) genericFieldType;
			    Type[] fieldArgTypes = aType.getActualTypeArguments();
			    for(Type fieldArgType : fieldArgTypes){
			    	managedClass = (Class<?>) fieldArgType;
			    	break;
			    }
			}
			
			if (managedClass == null) {
				logger.error("Field {} isn't paramertized", field.getName());
				throw new RuntimeException("Field " + field.getName() + " isn't paramertized");
			}
			return managedClass;
		}

	}
	
	
	/* Set the FSMAnnotationResolver to resolve all FSM annotations
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	public void postProcessBeanFactory(final ConfigurableListableBeanFactory reg)
			throws BeansException {
		DefaultListableBeanFactory  bf = (DefaultListableBeanFactory) reg;
		bf.setAutowireCandidateResolver(new FSMAnnotationResolver());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.appContext = applicationContext;
	}

	/* 
	 * Override postProcessBeanDefinitionRegistry to dynamically generate all the StatefulJ beans for each StatefulController
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry reg)
			throws BeansException {
		logger.debug("postProcessBeanDefinitionRegistry : enter");
		try {
			
			// Reflect over StatefulJ
			//
			Reflections reflections = new Reflections((Object[])this.packages);

			// Load up all Endpoint Binders
			//
			Map<String, EndpointBinder> binders = new HashMap<String, EndpointBinder>();
			loadEndpointBinders(reflections, binders);
		    
			// Load up all PersistenceSupportBeanFactories
			//
			Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories = new HashMap<Class<?>, PersistenceSupportBeanFactory>();
			loadPersistenceSupportBeanFactories(reflections, persistenceFactories);
			
			// Map Controllers and Entities
			//
			Map<String, Class<?>> controllerToEntityMapping = new HashMap<String, Class<?>>();
			Map<Class<?>, String> entityToRepositoryMappings = new HashMap<Class<?>, String>();
			
			mapControllerAndEntityClasses(
					reg, 
					controllerToEntityMapping, 
					entityToRepositoryMappings,
					entityToControllerMappings);

			// Iterate thru all StatefulControllers and build the framework 
			//
			for (Entry<String, Class<?>> entry : controllerToEntityMapping.entrySet()) {
				buildFramework(
						entry.getKey(), 
						entry.getValue(), 
						reg, 
						entityToRepositoryMappings, 
						binders,
						persistenceFactories);
			}

		} catch(Exception e) {
			throw new BeanCreationException("Unable to create bean", e);
		}
		logger.debug("postProcessBeanDefinitionRegistry : exit");
	}

	/**
	 * Iterate thru all beans and fetch the StatefulControllers
	 * 
	 * @param reg
	 * @return
	 * @throws ClassNotFoundException
	 */
	private void mapControllerAndEntityClasses(
			BeanDefinitionRegistry reg,
			Map<String, Class<?>> controllerToEntityMapping,
			Map<Class<?>, String> entityToRepositoryMapping,
			Map<Class<?>, Set<String>> entityToControllerMappings) throws ClassNotFoundException {
		
		// Loop thru the bean registry
		//
		for(String bfName : reg.getBeanDefinitionNames()) {

			BeanDefinition bf = reg.getBeanDefinition(bfName);
		
			if (bf.isAbstract()) {
				logger.debug("Skipping abstract bean " + bfName);
				continue;
			}
			
			Class<?> clazz = getClassFromBeanDefinition(bf, reg);

			if (clazz == null) {
				logger.debug("Unable to resolve class for bean " + bfName);
				continue;
			}
			
			// If it's a StatefulController, map controller to the entity and the entity to the controller
			//
			if (ReflectionUtils.isAnnotationPresent(clazz, StatefulController.class)) {
				mapEntityWithController(controllerToEntityMapping, entityToControllerMappings, bfName, clazz);
			}

			// Else, if the Bean is a Repository, then map the
			// Entity associated with the Repo to the PersistenceSupport object
			//
			else if (RepositoryFactoryBeanSupport.class.isAssignableFrom(clazz)) {
				mapEntityToRepository(entityToRepositoryMapping, bfName, bf);
			}
		}
	}

	/**
	 * @param entityToRepositoryMapping
	 * @param bfName
	 * @param bf
	 * @throws ClassNotFoundException
	 */
	private void mapEntityToRepository(Map<Class<?>, String> entityToRepositoryMapping,
			String bfName, BeanDefinition bf) throws ClassNotFoundException {
		
		// Determine the Entity Class associated with the Repo
		//
		String value = (String)bf.getPropertyValues().getPropertyValue("repositoryInterface").getValue();
		Class<?> repoInterface = Class.forName(value);
		Class<?> entityType = null;
		for(Type type : repoInterface.getGenericInterfaces()) {
			if (type instanceof ParameterizedType) {
				ParameterizedType parmType = (ParameterizedType)type;
				if (Repository.class.isAssignableFrom((Class<?>)parmType.getRawType()) &&
				    parmType.getActualTypeArguments() != null &&
				    parmType.getActualTypeArguments().length > 0) {
					entityType = (Class<?>)parmType.getActualTypeArguments()[0];
					break;
				}
			}
		}
		
		if (entityType == null) {
			throw new RuntimeException("Unable to determine Entity type for class " + repoInterface.getName());
		}
		
		// Map Entity to the RepositoryFactoryBeanSupport bean
		//
		logger.debug("Mapped \"{}\" to repo \"{}\", beanId=\"{}\"", entityType.getName(), value, bfName);
		
		entityToRepositoryMapping.put(entityType, bfName);
	}

	/**
	 * @param controllerToEntityMapping
	 * @param entityToControllerMappings
	 * @param bfName
	 * @param clazz
	 */
	private void mapEntityWithController(Map<String, Class<?>> controllerToEntityMapping,
			Map<Class<?>, Set<String>> entityToControllerMappings, String bfName,
			Class<?> clazz) {
		logger.debug("Found StatefulController, class = \"{}\", beanName = \"{}\"", clazz.getName(), bfName);

		// Ctrl -> Entity
		//
		controllerToEntityMapping.put(bfName, clazz); 
		
		// Entity -> Ctrls
		//
		Class<?> managedEntity = ReflectionUtils.getFirstClassAnnotation(clazz, StatefulController.class).clazz();
		Set<String> controllers = entityToControllerMappings.get(managedEntity);
		if (controllers == null) {
			controllers = new HashSet<String>();
			entityToControllerMappings.put(managedEntity, controllers);
		}
		controllers.add(bfName);
	}
	
	private void buildFramework(
			String statefulControllerBeanId, 
			Class<?> statefulControllerClass, 
			BeanDefinitionRegistry reg, 
			Map<Class<?>, String> entityToRepositoryMappings,
			Map<String, EndpointBinder> binders,
			Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories
			) throws CannotCompileException, IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		
		// Determine the managed class
		// 
		StatefulController scAnnotation = ReflectionUtils.getFirstClassAnnotation(statefulControllerClass, StatefulController.class);
		Class<?> managedClass = scAnnotation.clazz();
		
		// Is the the Controller and ManagedClass the same?  (DomainEntity)
		//
		boolean isDomainEntity = managedClass.equals(statefulControllerClass);
		
		// ReferenceFactory will generate all the necessary bean ids
		//
		ReferenceFactory referenceFactory = new ReferenceFactoryImpl(statefulControllerBeanId);
		
		// We need to map Transitions across all Methods
		//
		Map<String, Map<String, Method>> providersMappings = new HashMap<String, Map<String, Method>>();
		Map<Transition, Method> transitionMapping = new HashMap<Transition, Method>();
		Map<Transition, Method> anyMapping = new HashMap<Transition, Method>();
		Set<String> states = new HashSet<String>();
		Set<String> blockingStates = new HashSet<String>();

		// Fetch Repo info
		//
		String repoBeanId = getRepoId(entityToRepositoryMappings, managedClass);
		
		if (repoBeanId == null) {
			throw new RuntimeException("Unable to determine Repository for class " + managedClass.getName());
		}
		BeanDefinition repoBeanDefinitionFactory = reg.getBeanDefinition(repoBeanId);
		Class<?> repoClassName = getClassFromBeanClassName(repoBeanDefinitionFactory);

		// Fetch the PersistenceFactory
		//
		PersistenceSupportBeanFactory factory = persistenceFactories.get(repoClassName);
		
		// Map the Events and Transitions for the Controller
		//
		mapEventsTransitionsAndStates(
				statefulControllerClass, 
				providersMappings, 
				transitionMapping, 
				anyMapping, 
				states,
				blockingStates);
		
		// Iterate thru the providers - building and registering each Binder
		//
		for(Entry<String, Map<String, Method>> entry : providersMappings.entrySet()) {
			
			// Fetch the binder
			//
			EndpointBinder binder = binders.get(entry.getKey());

			// Check if we found the binder
			//
			if (binder == null) {
				logger.error("Unable to locate binder: {}", entry.getKey());
				throw new RuntimeException("Unable to locate binder: " + entry.getKey());
			}
			
			// Build out the Binder Class
			//
			Class<?> binderClass = binder.bindEndpoints(
					statefulControllerBeanId, 
					statefulControllerClass, 
					factory.getIdType(),
					isDomainEntity,
					entry.getValue(), 
					referenceFactory);

			// Add the new Binder Class to the Bean Registry
			//
			registerBinderBean(entry.getKey(), referenceFactory, binderClass, reg);
		}
		
		// -- Build the FSM infrastructure --
		
		// Build out a set of States
		//
		List<RuntimeBeanReference> stateBeans = new ManagedList<RuntimeBeanReference>();
		for(String state : states) {
			logger.debug("Registering state \"{}\"", state);
			String stateId = registerState(
					referenceFactory,
					statefulControllerClass, 
					state,
					blockingStates.contains(state),
					reg);
			stateBeans.add(new RuntimeBeanReference(stateId));
		}
		
		// Build out the Action classes and the Transitions
		//
		RuntimeBeanReference controllerRef = new RuntimeBeanReference(statefulControllerBeanId);
		int cnt = 1;
		List<String> transitionIds = new LinkedList<String>();
		for(Entry<Transition, Method> entry : anyMapping.entrySet()) {
			for (String state : states) {
				Transition t = entry.getKey();
				String from = state;
				String to = (t.to().equals(Transition.ANY_STATE)) ? state : entry.getKey().to();
				String transitionId = referenceFactory.getTransitionId(cnt++);
				boolean reload = t.reload();
				registerActionAndTransition(
						referenceFactory,
						statefulControllerClass, 
						from, 
						to, 
						reload,
						entry.getKey(), 
						entry.getValue(), 
						isDomainEntity,
						controllerRef, 
						transitionId, 
						reg);
				transitionIds.add(transitionId);
			}
		}
		for(Entry<Transition, Method> entry : transitionMapping.entrySet()) {
			Transition t = entry.getKey();
			boolean reload = t.reload();
			String transitionId = referenceFactory.getTransitionId(cnt++);
			registerActionAndTransition(
					referenceFactory,
					statefulControllerClass, 
					entry.getKey().from(), 
					entry.getKey().to(), 
					reload,
					entry.getKey(), 
					entry.getValue(), 
					isDomainEntity,
					controllerRef, 
					transitionId, 
					reg);
			transitionIds.add(transitionId);
		}
		
		// Build out the Managed Entity Factory Bean
		//
		String factoryId = registerFactoryBean(
				referenceFactory, 
				factory,
				scAnnotation, 
				reg);

		// Build out the Managed Entity Finder Bean
		//
		String finderId = registerFinderBean(
				referenceFactory, 
				factory,
				scAnnotation, 
				repoBeanId,
				reg);

		// Build out the Managed Entity State Persister Bean
		//
		String persisterId = registerPersisterBean(
				referenceFactory, 
				factory, 
				scAnnotation, 
				managedClass, 
				repoBeanId,
				repoBeanDefinitionFactory,
				stateBeans, 
				reg);

		// Build out the FSM Bean
		//
		String fsmBeanId = registerFSM(
				referenceFactory,
				statefulControllerClass, 
				scAnnotation,
				persisterId, 
				managedClass, 
				finderId, 
				factory.getIdAnnotationType(),
				reg);

		// Build out the StatefulFSM Bean
		//
		String statefulFSMBeanId = registerStatefulFSMBean(
				referenceFactory,
				managedClass, 
				fsmBeanId, 
				factoryId, 
				transitionIds,
				reg);

		// Build out the FSMHarness Bean
		//
		registerFSMHarness(
				referenceFactory,
				factory, 
				managedClass, 
				statefulFSMBeanId, 
				factoryId, 
				finderId, 
				repoBeanDefinitionFactory,
				reg);
	}
	
	private void mapEventsTransitionsAndStates(
			Class<?> statefulControllerClass, 
			Map<String, Map<String, Method>> providerMappings,
			Map<Transition, Method> transitionMapping,
			Map<Transition, Method> anyMapping,
			Set<String> states,
			Set<String> blockingStates) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		
		// Walk up the Class hierarchy building out the FSM
		//
		if (statefulControllerClass == null) {
			return;
		} else {
			mapEventsTransitionsAndStates(
				statefulControllerClass.getSuperclass(), 
				providerMappings,
				transitionMapping,
				anyMapping,
				states,
				blockingStates
			);
		}
		
		logger.debug("Mapping events and transitions for {}", statefulControllerClass);

		// Pull StateController Annotation
		//
		StatefulController ctrlAnnotation = statefulControllerClass.getAnnotation(StatefulController.class);

		if (ctrlAnnotation != null) {
			// Add Start State
			//
			states.add(ctrlAnnotation.startState());
			
			// Add the list of BlockingStates
			//
			blockingStates.addAll(Arrays.asList(ctrlAnnotation.blockingStates()));

			// Map the NOOP Transitions
			//
			for (Transition transition : ctrlAnnotation.noops()) {
				mapTransition(
						transition, 
						null,
						providerMappings,
						transitionMapping, 
						anyMapping,
						states);
			}
		}
		
		// TODO : As we map the events, we need to make sure that the method signature and return
		// types of all the handlers for the event are the same
		
		for(Method method : statefulControllerClass.getDeclaredMethods()) {
			
			// Map the set of transitions as defined by the Transitions annotation
			//
			Transitions transitions = method.getAnnotation(Transitions.class);
			if (transitions != null) {
				for(Transition transition : transitions.value()) {
					mapTransition(
							transition, 
							method,
							providerMappings,
							transitionMapping, 
							anyMapping,
							states);
				}
			}
			
			// Map the Transition annotation
			//
			Transition transition = method.getAnnotation(Transition.class);
			if (transition != null) {
				mapTransition(
						transition, 
						method,
						providerMappings,
						transitionMapping, 
						anyMapping,
						states);
			}
		}
	}
	
	private void mapTransition(
			Transition transition, 
			Method method,
			Map<String, Map<String, Method>> providerMappings,
			Map<Transition, Method> transitionMapping,
			Map<Transition, Method> anyMapping,
			Set<String> states) {
		
		logger.debug(
				"Mapping {}:{}->{}",
				transition.from(),
				transition.event(),
				transition.to());
		
		Pair<String, String> providerEvent = parseEvent(transition.event()); 
		String provider = providerEvent.getLeft();
		if (provider != null) {
			Map<String, Method> eventMapping = providerMappings.get(provider);
			if (eventMapping == null) {
				eventMapping = new HashMap<String, Method>();
				providerMappings.put(provider, eventMapping);
			}
			eventMapping.put(providerEvent.getRight(), method);
		}

		if (!transition.from().equals(Transition.ANY_STATE)) {
			states.add(transition.from());
			transitionMapping.put(transition, method);
		} else {
			anyMapping.put(transition, method);
		}
		if (!transition.to().equals(Transition.ANY_STATE)) {
			states.add(transition.to());
		}
	}
	
	private Pair<String, String> parseEvent(String event) {
		Matcher matcher = this.binder.matcher(event);
		if (!matcher.matches()) {
			throw new RuntimeException("Unable to parse event=" + event);
		}
		return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
	}
	
	private void registerActionAndTransition(
			ReferenceFactory referenceFactory,
			Class<?> clazz, 
			String from, 
			String to, 
			boolean reload,
			Transition transition, 
			Method method, 
			boolean isDomainEntity,
			RuntimeBeanReference controllerRef, 
			String transitionId, 
			BeanDefinitionRegistry reg) {
		
		// Remap to="Any" to to=from
		//
		to = (Transition.ANY_STATE.equals(to)) ? from : to;
		
		logger.debug(
				"Registered: {}({})->{}/{}",
				from,
				transition.event(),
				to,
				(method == null) ? "noop" : method.getName());
		
		// Build the Action Bean
		//
		RuntimeBeanReference actionRef = null;
		if (method != null) {
			String actionId = referenceFactory.getActionId(method);
			if (!reg.isBeanNameInUse(actionId)) {
				registerMethodInvocationAction(referenceFactory, method,
						isDomainEntity, controllerRef, reg, actionId);
			}
			actionRef = new RuntimeBeanReference(actionId);
		}
		
		registerTransition(referenceFactory, from, to, reload, transition,
				transitionId, reg, actionRef);
	}

	/**
	 * @param referenceFactory
	 * @param from
	 * @param to
	 * @param reload
	 * @param transition
	 * @param transitionId
	 * @param reg
	 * @param actionRef
	 */
	private void registerTransition(ReferenceFactory referenceFactory,
			String from, String to, boolean reload, Transition transition,
			String transitionId, BeanDefinitionRegistry reg,
			RuntimeBeanReference actionRef) {
		// Build the Transition Bean
		//
		BeanDefinition transitionBean = BeanDefinitionBuilder
				.genericBeanDefinition(TransitionImpl.class)
				.getBeanDefinition();

		String fromId = referenceFactory.getStateId(from);
		String toId = referenceFactory.getStateId(to);
		Pair<String, String> providerEvent = parseEvent(transition.event());

		ConstructorArgumentValues args = transitionBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fromId));
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(toId));
		args.addIndexedArgumentValue(2, providerEvent.getRight());
		args.addIndexedArgumentValue(3, actionRef);
		args.addIndexedArgumentValue(4, 
				(transition.from().equals(Transition.ANY_STATE) && 
				 transition.to().equals(Transition.ANY_STATE)));
		args.addIndexedArgumentValue(5, reload);
		reg.registerBeanDefinition(transitionId, transitionBean);
	}

	/**
	 * @param referenceFactory
	 * @param method
	 * @param isDomainEntity
	 * @param controllerRef
	 * @param reg
	 * @param actionId
	 */
	private void registerMethodInvocationAction(
			ReferenceFactory referenceFactory, Method method,
			boolean isDomainEntity, RuntimeBeanReference controllerRef,
			BeanDefinitionRegistry reg, String actionId) {
		// Choose the type of invocationAction based off of 
		// whether the controller is a DomainEntity
		//
		Class<?> methodInvocationAction = (isDomainEntity) ?
				DomainEntityMethodInvocationAction.class :
				MethodInvocationAction.class;
		
		BeanDefinition actionBean = BeanDefinitionBuilder
				.genericBeanDefinition(methodInvocationAction)
				.getBeanDefinition();
		
		ConstructorArgumentValues args = actionBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, method.getName());
		args.addIndexedArgumentValue(1, method.getParameterTypes());
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(referenceFactory.getFSMId()));
		 
		if (!isDomainEntity) {
			args.addIndexedArgumentValue(3, controllerRef);
		}
		
		reg.registerBeanDefinition(actionId, actionBean);
	}

	private String registerState(
			ReferenceFactory referenceFactory,
			Class<?> statefulControllerClass, 
			String state, 
			boolean isBlocking,
			BeanDefinitionRegistry reg) {

		String stateId = referenceFactory.getStateId(state);
		BeanDefinition stateBean = BeanDefinitionBuilder
				.genericBeanDefinition(StateImpl.class)
				.getBeanDefinition();
		
		ConstructorArgumentValues args = stateBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, state);
		args.addIndexedArgumentValue(1, false);
		args.addIndexedArgumentValue(2, isBlocking);
		
		reg.registerBeanDefinition(stateId, stateBean);
		
		return stateId;
	}
	
	private String registerFSM(
			ReferenceFactory referenceFactory,
			Class<?> statefulControllerClass, 
			StatefulController scAnnotation,
			String persisterId, 
			Class<?> managedClass, 
			String finderId, 
			Class<? extends Annotation> idAnnotationType,
			BeanDefinitionRegistry reg) {
		int retryAttempts = scAnnotation.retryAttempts();
		int retryInterval = scAnnotation.retryInterval();
		
		String fsmBeanId = referenceFactory.getFSMId();
		BeanDefinition fsmBean = BeanDefinitionBuilder
				.genericBeanDefinition(FSM.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = fsmBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, fsmBeanId);
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(persisterId));
		args.addIndexedArgumentValue(2, retryAttempts);
		args.addIndexedArgumentValue(3, retryInterval);
		args.addIndexedArgumentValue(4, managedClass);
		args.addIndexedArgumentValue(5, idAnnotationType);
		args.addIndexedArgumentValue(6, new RuntimeBeanReference(finderId));
		args.addIndexedArgumentValue(7, this.appContext);

		reg.registerBeanDefinition(fsmBeanId, fsmBean);
		return fsmBeanId;
	}

	private String registerStatefulFSMBean(
			ReferenceFactory referenceFactory,
			Class<?> statefulClass, 
			String fsmBeanId, 
			String factoryId,
			List<String> transitionIds,
			BeanDefinitionRegistry reg) {
		String statefulFSMBeanId = referenceFactory.getStatefulFSMId();
		BeanDefinition statefulFSMBean = BeanDefinitionBuilder
				.genericBeanDefinition(StatefulFSMImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = statefulFSMBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
		args.addIndexedArgumentValue(1, statefulClass);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
		reg.registerBeanDefinition(statefulFSMBeanId, statefulFSMBean);
		statefulFSMBean.setDependsOn(transitionIds.toArray(new String[]{}));
		return statefulFSMBeanId;
	}

	private String registerBinderBean(
			String key,
			ReferenceFactory referenceFactory,
			Class<?> binderClass, 
			BeanDefinitionRegistry reg) {
		BeanDefinition def = BeanDefinitionBuilder
				.genericBeanDefinition(binderClass)
				.getBeanDefinition();
		String binderId = referenceFactory.getBinderId(key);
		reg.registerBeanDefinition(binderId, def);
		return binderId;
	}
	
	private String registerFactoryBean(
			ReferenceFactory referenceFactory,
			PersistenceSupportBeanFactory persistenceFactory,
			StatefulController statefulContollerAnnotation, 
			BeanDefinitionRegistry reg) {

		String factoryId = statefulContollerAnnotation.factoryId();
		
		if (StringUtils.isEmpty(factoryId)) {
			if (persistenceFactory == null) {
				throw new RuntimeException("PersistenceFactory is undefined and no factory bean was specified in the StatefulController Annotation");
			}
			factoryId = referenceFactory.getFactoryId();
			reg.registerBeanDefinition(
					factoryId, 
					persistenceFactory.buildFactoryBean(statefulContollerAnnotation.clazz()));
		}
		
		return factoryId;
	}

	private String registerFinderBean(
			ReferenceFactory referenceFactory,
			PersistenceSupportBeanFactory persistenceFactory,
			StatefulController statefulContollerAnnotation, 
			String repoBeanId,
			BeanDefinitionRegistry reg) {

		String finderId = statefulContollerAnnotation.finderId();
		
		if (StringUtils.isEmpty(finderId)) {
			if (persistenceFactory == null) {
				throw new RuntimeException("PersistenceFactory is undefined and no finder bean was specified in the StatefulController Annotation");
			}
			if (StringUtils.isEmpty(repoBeanId)) {
				throw new RuntimeException("No Repository is defined for " + statefulContollerAnnotation.clazz());
			}
			finderId = referenceFactory.getFinderId();
			reg.registerBeanDefinition(
					finderId, 
					persistenceFactory.buildFinderBean(repoBeanId));
		}
		
		return finderId;
	}

	private String registerPersisterBean(
			ReferenceFactory referenceFactory,
			PersistenceSupportBeanFactory persistenceFactory,
			StatefulController statefulContollerAnnotation,
			Class<?> statefulClass,
			String repoBeanId,
			BeanDefinition repoBeanDefinitionFactory,
			List<RuntimeBeanReference> stateBeans,
			BeanDefinitionRegistry reg) {

		String persisterId = statefulContollerAnnotation.persisterId();
		
		if (StringUtils.isEmpty(persisterId)) {
			if (persistenceFactory == null) {
				throw new RuntimeException("PersistenceFactory is undefined and no persister bean was specified in the StatefulController Annotation");
			}
			String startStateId = referenceFactory.getStateId(statefulContollerAnnotation.startState());
			persisterId = referenceFactory.getPersisterId();
			reg.registerBeanDefinition(
					persisterId, 
					persistenceFactory.buildPersisterBean(
							statefulClass, 
							repoBeanId,
							repoBeanDefinitionFactory,
							statefulContollerAnnotation.stateField(),
							startStateId, 
							stateBeans));
		}
		
		return persisterId;
	}
	
	private String registerFSMHarness(
				ReferenceFactory referenceFactory,
				PersistenceSupportBeanFactory persistenceFactory,
				Class<?> statefulClass, 
				String fsmBeanId, 
				String factoryId, 
				String finderId, 
				BeanDefinition repoBeanFactory,
				BeanDefinitionRegistry reg) {
		String fsmHarnessId = referenceFactory.getFSMHarnessId();
		reg.registerBeanDefinition(
				fsmHarnessId, 
				persistenceFactory.buildFSMHarnessBean(
						statefulClass, 
						fsmBeanId, 
						factoryId, 
						finderId,
						repoBeanFactory));
		return fsmHarnessId;
	}
	
	private String getRepoId(Map<Class<?>, String> entityToRepositoryMappings, Class<?> clazz) {
		if (clazz != null) {
			String id = entityToRepositoryMappings.get(clazz);
			if (id != null) {
				return id;
			}
			id = getRepoId(entityToRepositoryMappings, clazz.getSuperclass());
			if (id != null) {
				return id;
			}
			for (Class<?> interfaze : clazz.getInterfaces()) {
				id = getRepoId(entityToRepositoryMappings, interfaze);
				if (id != null) {
					return id;
				}
			}
		}
		return null;
	}
	
	private Class<?> getClassFromBeanDefinition(BeanDefinition bf, BeanDefinitionRegistry reg) throws ClassNotFoundException {
		Class<?> clazz = null;
		
		if (bf.getBeanClassName() == null) {
			clazz = getClassFromFactoryMethod(bf, reg);
		} else {
			clazz = getClassFromBeanClassName(bf);
		}
		
		if (clazz == null) {
			clazz = getClassFromParentBean(bf, reg);
		}
		
		return clazz;
	}

	/**
	 * @param bf
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?> getClassFromBeanClassName(BeanDefinition bf)
			throws ClassNotFoundException {
		return Class.forName(bf.getBeanClassName());
	}

	/**
	 * @param bf
	 * @param reg
	 * @param clazz
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?> getClassFromParentBean(BeanDefinition bf, BeanDefinitionRegistry reg)
			throws ClassNotFoundException {
		Class<?> clazz = null;
		String parentBeanName = bf.getParentName();
		if (parentBeanName != null) {
			BeanDefinition parent = reg.getBeanDefinition(parentBeanName);
			if (parent != null) {
				clazz = this.getClassFromBeanDefinition(parent, reg);
			}
		}
		return clazz;
	}

	/**
	 * @param bf
	 * @param reg
	 * @param clazz
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?> getClassFromFactoryMethod(BeanDefinition bf, BeanDefinitionRegistry reg)
			throws ClassNotFoundException {
		Class<?> clazz = null;
		String factoryBeanName = bf.getFactoryBeanName();
		if (factoryBeanName != null) {
			BeanDefinition factory = reg.getBeanDefinition(factoryBeanName);
			if (factory != null) {
				String factoryClassName = factory.getBeanClassName();
				Class<?> factoryClass = Class.forName(factoryClassName);
				List<Method> methods = new LinkedList<Method>();
				methods.addAll(Arrays.asList(factoryClass.getMethods()));
				methods.addAll(Arrays.asList(factoryClass.getDeclaredMethods()));
				for (Method method : methods) {
					method.setAccessible(true);
					if (method.getName().equals(bf.getFactoryMethodName())) {
						clazz = method.getReturnType();
						break;
					}
				}
			}
		}
		return clazz;
	}
	
	/**
	 * @param reflections
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private void loadPersistenceSupportBeanFactories(Reflections reflections, Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories)
			throws InstantiationException, IllegalAccessException {
		Set<Class<? extends PersistenceSupportBeanFactory>> persistenceFactoryTypes = reflections.getSubTypesOf(PersistenceSupportBeanFactory.class);
		for(Class<?> persistenceFactoryType : persistenceFactoryTypes) {
			if (!Modifier.isAbstract(persistenceFactoryType.getModifiers())) {
				PersistenceSupportBeanFactory factory = (PersistenceSupportBeanFactory)persistenceFactoryType.newInstance();
				persistenceFactories.put(factory.getKey(), factory);
			}
		}
	}

	/**
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private void loadEndpointBinders(Reflections reflections, Map<String, EndpointBinder> binders) throws InstantiationException,
			IllegalAccessException {
		Set<Class<? extends EndpointBinder>> endpointBinders = reflections.getSubTypesOf(EndpointBinder.class);
		for(Class<?> binderClass : endpointBinders) {
			if (!Modifier.isAbstract(binderClass.getModifiers())) {
				EndpointBinder binder = (EndpointBinder)binderClass.newInstance();
				binders.put(binder.getKey(), binder);
			}
		}
	}	
}