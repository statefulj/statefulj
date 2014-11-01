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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.springframework.util.CollectionUtils;
import org.statefulj.framework.core.actions.MethodInvocationAction;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.fsm.FSM;
import org.statefulj.framework.core.fsm.TransitionImpl;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.core.model.impl.StatefulFSMImpl;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;
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
	
	Logger logger = LoggerFactory.getLogger(StatefulFactory.class);
	
	private final Pattern binder = Pattern.compile("(([^:]*):)?(.*)");

	private Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories = new HashMap<Class<?>, PersistenceSupportBeanFactory>();
	private Map<String, EndpointBinder> binders = new HashMap<String, EndpointBinder>();
	private Map<Class<?>, Set<String>> entityToControllers = new HashMap<Class<?>, Set<String>>();
	
	// Resolver that injects the FSM for a given controller.  It is inferred by the ClassType or will use the bean Id specified by the value of the 
	// FSM Annotation
	//
	class FSMAnnotationResolver extends QualifierAnnotationAutowireCandidateResolver {
		
		@Override
		public Object getSuggestedValue(DependencyDescriptor descriptor) {
			Object suggested = null;
			Field field = descriptor.getField();
			
			if (field != null && field.getType().isAssignableFrom(StatefulFSM.class)) {
				
				org.statefulj.framework.core.annotations.FSM fsmAnnotation = field.getAnnotation(org.statefulj.framework.core.annotations.FSM.class);
				String controllerId = (fsmAnnotation != null ) ? fsmAnnotation.value() : null;
				
				if (StringUtils.isEmpty(controllerId)) {
	
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
					
					Set<String> controllers = entityToControllers.get(managedClass);
					if (controllers == null) {
						throw new RuntimeException("Unable to resolve FSM for field " + field.getName());
					}
					if (controllers.size() > 1) {
						throw new RuntimeException("Ambiguous fsm for " + field.getName());
					}
					controllerId = controllers.iterator().next();
				}
				ReferenceFactory refFactory = new ReferenceFactoryImpl(controllerId);
				suggested = appContext.getBean(refFactory.getStatefulFSMId());
			} 
			return (suggested != null) ? suggested : super.getSuggestedValue(descriptor);
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

	/* 
	 * Override postProcessBeanDefinitionRegistry to dynamically generate all the StatefulJ beans for each StatefulController
	 * s
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry reg)
			throws BeansException {
		logger.debug("postProcessBeanDefinitionRegistry : enter");
		try {
			
			// Load up all Endpoint Binders
			//
			Reflections reflections = new Reflections("org.statefulj");
			Set<Class<? extends EndpointBinder>> endpointBinders = reflections.getSubTypesOf(EndpointBinder.class);
			for(Class<?> binderClass : endpointBinders) {
				if (!Modifier.isAbstract(binderClass.getModifiers())) {
					EndpointBinder binder = (EndpointBinder)binderClass.newInstance();
					binders.put(binder.getKey(), binder);
				}
			}
		    
			// Load up all PersistenceSupportBeanFactories
			//
			Set<Class<? extends PersistenceSupportBeanFactory>> persistenceFactoryTypes = reflections.getSubTypesOf(PersistenceSupportBeanFactory.class);
			for(Class<?> persistenceFactories : persistenceFactoryTypes) {
				if (!Modifier.isAbstract(persistenceFactories.getModifiers())) {
					PersistenceSupportBeanFactory factory = (PersistenceSupportBeanFactory)persistenceFactories.newInstance();
					this.persistenceFactories.put(factory.getKey(), factory);
				}
			}
		    
			// Map Controllers and Entities
			//
			Map<String, Class<?>> controllerMapping = new HashMap<String, Class<?>>();
			Map<Class<?>, String> entityMappings = new HashMap<Class<?>, String>();
			
			mapControllerAndEntityClasses(reg, controllerMapping, entityMappings, entityToControllers);

			// Iterate thru all StatefulControllers and build the framework 
			//
			for (Entry<String, Class<?>> entry : controllerMapping.entrySet()) {
				buildFramework(entry.getKey(), entry.getValue(), reg, entityMappings);
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
			Map<String, Class<?>> controllerMapping,
			Map<Class<?>, String> entityMapping,
			Map<Class<?>, Set<String>> entityToControllers) throws ClassNotFoundException {
		
		// Loop thru the bean registry
		//
		for(String bfName : reg.getBeanDefinitionNames()) {
			
			BeanDefinition bf = reg.getBeanDefinition(bfName);
			Class<?> clazz = getBeanClass(bf, reg);

			if (clazz == null) {
				throw new RuntimeException("Unable to resolve class for bean " + bfName);
			}
			
			// If it's a StatefulController, map controller to the entity and the entity to the controller
			//
			if (clazz.isAnnotationPresent(StatefulController.class)) {
				
				logger.debug("Found StatefulController, class = \"{}\"", clazz.getName());

				// Ctrl -> Entity
				//
				controllerMapping.put(bfName, clazz); 
				
				// Entity -> Ctrls
				//
				Class<?> managedEntity = ((StatefulController)clazz.getAnnotation(StatefulController.class)).clazz();
				Set<String> controllers = entityToControllers.get(managedEntity);
				if (controllers == null) {
					controllers = new HashSet<String>();
					entityToControllers.put(managedEntity, controllers);
				}
				controllers.add(bfName);
			}
			

			// Else, if the Bean is a Repository, then map the
			// Entity associated with the Repo to the PersistenceSupport object
			//
			else if (RepositoryFactoryBeanSupport.class.isAssignableFrom(clazz)) {
				
				// Determine the Entity Class associated with the Repo
				//
				String value = (String)bf.getPropertyValues().getPropertyValue("repositoryInterface").getValue();
				Class<?> repoInterface = Class.forName(value);
				Class<?> entityType = null;
				for(Type type : repoInterface.getGenericInterfaces()) {
					if (type instanceof ParameterizedType) {
						ParameterizedType parmType = (ParameterizedType)type;
						if (Repository.class.isAssignableFrom((Class<?>)parmType.getRawType())) {
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
				entityMapping.put(entityType, bfName);
			}
		}
	}
	
	private void buildFramework(
			String controllerBeanId, 
			Class<?> statefulControllerClass, 
			BeanDefinitionRegistry reg, 
			Map<Class<?>, String> entityMappings) throws CannotCompileException, IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		
		// Determine the managed class
		// 
		Class<?> statefulClass = statefulControllerClass.getAnnotation(StatefulController.class).clazz();
		ReferenceFactory referenceFactory = new ReferenceFactoryImpl(controllerBeanId);
		
		// Gather all the events from across all the methods
		//
		Map<String, Map<String, Method>> providersMappings = new HashMap<String, Map<String, Method>>();
		Map<Transition, Method> transitionMapping = new HashMap<Transition, Method>();
		Map<Transition, Method> anyMapping = new HashMap<Transition, Method>();
		Set<String> states = new HashSet<String>();
		Set<String> blockingStates = new HashSet<String>();

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
			EndpointBinder binder = this.binders.get(entry.getKey());

			// Check if we found the binder
			//
			if (binder == null) {
				logger.error("Unable to locate binder: {}", entry.getKey());
				throw new RuntimeException("Unable to locate binder: " + entry.getKey());
			}
			
			// Build out the Binder Class
			//
			Class<?> binderClass = binder.bindEndpoints(controllerBeanId, statefulControllerClass, entry.getValue(), referenceFactory);

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
		RuntimeBeanReference controllerRef = new RuntimeBeanReference(controllerBeanId);
		int cnt = 1;
		for(Entry<Transition, Method> entry : anyMapping.entrySet()) {
			for (String state : states) {
				String from = state;
				String to = (entry.getKey().to().equals(Transition.ANY_STATE)) ? state : entry.getKey().to();
				registerActionAndTransition(
						referenceFactory,
						statefulControllerClass, 
						from, 
						to, 
						entry.getKey(), 
						entry.getValue(), 
						controllerRef, 
						cnt, 
						reg);
				cnt++;
			}
		}
		for(Entry<Transition, Method> entry : transitionMapping.entrySet()) {
			registerActionAndTransition(
					referenceFactory,
					statefulControllerClass, 
					entry.getKey().from(), 
					entry.getKey().to(), 
					entry.getKey(), 
					entry.getValue(), 
					controllerRef, 
					cnt, 
					reg);
			cnt++;
		}
		
		// Fetch Repo info
		//
		String repoBeanId = getRepoId(entityMappings, statefulClass);
		
		if (repoBeanId == null) {
			throw new RuntimeException("Unable to determine Repository for class " + statefulClass.getName());
		}
		
		BeanDefinition repoBean = reg.getBeanDefinition(repoBeanId);
		Class<?> repoBeanClass = Class.forName(repoBean.getBeanClassName());

		// Fetch the PersistenceFactory
		PersistenceSupportBeanFactory factory = this.persistenceFactories.get(repoBeanClass);
		
		// Fetch the StatefulController Annotation
		//
		StatefulController statefulContollerAnnotation = statefulControllerClass.getAnnotation(StatefulController.class);

		// Build out the Managed Entity Factory Bean
		//
		String factoryId = registerFactoryBean(
				referenceFactory, 
				factory,
				statefulContollerAnnotation, 
				reg);

		// Build out the Managed Entity Finder Bean
		//
		String finderId = registerFinderBean(
				referenceFactory, 
				factory,
				statefulContollerAnnotation, 
				repoBeanId,
				reg);

		// Build out the Managed Entity State Persister Bean
		//
		String persisterId = registerPersisterBean(
				referenceFactory, 
				factory, 
				statefulContollerAnnotation, 
				statefulClass, 
				repoBeanId,
				stateBeans, 
				reg);

		// Build out the FSM Bean
		//
		String fsmBeanId = registerFSM(
				referenceFactory,
				statefulControllerClass, 
				persisterId, 
				reg);

		// Build out the StatefulFSM Bean
		//
		String statefulFSMBeanId = registerStatefulFSMBean(
				referenceFactory,
				statefulClass, 
				fsmBeanId, 
				factoryId, 
				reg);

		// Build out the FSMHarness Bean
		//
		registerFSMHarness(
				referenceFactory,
				factory, 
				statefulClass, 
				statefulFSMBeanId, 
				factoryId, 
				finderId, 
				reg);

	}
	
	private void registerActionAndTransition(
			ReferenceFactory referenceFactory,
			Class<?> clazz, 
			String from, 
			String to, 
			Transition transition, 
			Method method, 
			RuntimeBeanReference controllerRef, 
			int cnt, 
			BeanDefinitionRegistry reg) {
		
		// Remap to="Any" to to=from
		//
		to = (Transition.ANY_STATE.equals(to)) ? from : to;
		
		logger.debug(
				"Registered: {}({})->{}({})",
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
				BeanDefinition actionBean = BeanDefinitionBuilder
						.genericBeanDefinition(MethodInvocationAction.class)
						.getBeanDefinition();
				ConstructorArgumentValues args = actionBean.getConstructorArgumentValues();
				args.addIndexedArgumentValue(0, controllerRef);
				args.addIndexedArgumentValue(1, method.getName());
				args.addIndexedArgumentValue(2, method.getParameterTypes());
				args.addIndexedArgumentValue(3, new RuntimeBeanReference(referenceFactory.getFSMId()));
				reg.registerBeanDefinition(actionId, actionBean);
			}
			actionRef = new RuntimeBeanReference(actionId);
		}
		
		// Build the Transition Bean
		//
		String transitionId = referenceFactory.getTransitionId(cnt);
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
		
		reg.registerBeanDefinition(transitionId, transitionBean);
	}

	private void mapEventsTransitionsAndStates(
			Class<?> statefulControllerClass, 
			Map<String, Map<String, Method>> providerMappings,
			Map<Transition, Method> transitionMapping,
			Map<Transition, Method> anyMapping,
			Set<String> states,
			Set<String> blockingStates) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		logger.debug("Mapping events and transitions for {}", statefulControllerClass);
		
		StatefulController ctrlAnnotation = statefulControllerClass.getAnnotation(StatefulController.class);

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
			String persisterId, 
			BeanDefinitionRegistry reg) {
		String fsmBeanId = referenceFactory.getFSMId();
		BeanDefinition fsmBean = BeanDefinitionBuilder
				.genericBeanDefinition(FSM.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = fsmBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, fsmBeanId);
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(persisterId));
		reg.registerBeanDefinition(fsmBeanId, fsmBean);
		return fsmBeanId;
	}

	private String registerStatefulFSMBean(
			ReferenceFactory referenceFactory,
			Class<?> statefulClass, 
			String fsmBeanId, 
			String factoryId,
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
				BeanDefinitionRegistry reg) {
		String fsmHarnessId = referenceFactory.getFSMHarnessId();
		reg.registerBeanDefinition(
				fsmHarnessId, 
				persistenceFactory.buildFSMHarnessBean(
						statefulClass, 
						fsmBeanId, 
						factoryId, 
						finderId));
		return fsmHarnessId;
	}
	
	private String getRepoId(Map<Class<?>, String> entityMappings, Class<?> clazz) {
		if (clazz != null) {
			String id = entityMappings.get(clazz);
			if (id != null) {
				return id;
			}
			id = getRepoId(entityMappings, clazz.getSuperclass());
			if (id != null) {
				return id;
			}
			for (Class<?> interfaze : clazz.getInterfaces()) {
				id = getRepoId(entityMappings, interfaze);
				if (id != null) {
					return id;
				}
			}
		}
		return null;
	}
	
	private Class<?> getBeanClass(BeanDefinition bf, BeanDefinitionRegistry reg) throws ClassNotFoundException {
		Class<?> clazz = null;
		if (bf.getBeanClassName() == null) {
			BeanDefinition factory = reg.getBeanDefinition(bf.getFactoryBeanName());
			String factoryClassName = factory.getBeanClassName();
			Class<?> factoryClass = Class.forName(factoryClassName);
			for (Method method : factoryClass.getMethods()) {
				if (method.getName().equals(bf.getFactoryMethodName())) {
					clazz = method.getReturnType();
					break;
				}
			}
		} else {
			clazz = Class.forName(bf.getBeanClassName());
		}
		return clazz;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.appContext = applicationContext;
	}

	private boolean contains(Annotation[] annotations, Class<? extends Annotation> clazz) {
		boolean contains = false;
		for(Annotation annotation : annotations) {
			if (clazz.isAssignableFrom(annotation.getClass())) {
				contains = true;
				break;
			}
		}
		return contains;
	}

}