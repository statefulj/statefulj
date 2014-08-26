package org.statefulj.framework.core;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.statefulj.framework.core.actions.MethodInvocationAction;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.fsm.FSM;
import org.statefulj.framework.core.fsm.TransitionImpl;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
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
public class StatefulFactory implements BeanDefinitionRegistryPostProcessor {
	
	Logger logger = LoggerFactory.getLogger(StatefulFactory.class);
	
	private final Pattern binder = Pattern.compile("(([^:]*):)?(.*)");

	public static String MVC_SUFFIX = "MVCProxy";
	public static String FSM_SUFFIX = "FSM";
	public static String STATE_SUFFIX = "State";
	public static String PERSISTENCE_SUPPORT_SUFFIX = "PersistenceSupport";
	
	private ReferenceFactory referenceFactory = new ReferenceFactoryImpl();
	
	private Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories = new HashMap<Class<?>, PersistenceSupportBeanFactory>();
	private Map<String, EndpointBinder> binders = new HashMap<String, EndpointBinder>();

	public void postProcessBeanFactory(ConfigurableListableBeanFactory reg)
			throws BeansException {
	}

	// TODO : Move creation of bean Ids into the Reference Factory
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
			mapControllerAndEntityClasses(reg, controllerMapping, entityMappings);

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
			Map<Class<?>, String> entityMapping) throws ClassNotFoundException {
		
		// Loop thru the bean registry
		//
		for(String bfName : reg.getBeanDefinitionNames()) {
			
			BeanDefinition bf = reg.getBeanDefinition(bfName);
			Class<?> clazz = Class.forName(bf.getBeanClassName());
			
			// If it's a StatefulController, add it to the mapping
			//
			if (clazz.isAnnotationPresent(StatefulController.class)) {
				
				logger.debug("mapControllerAndEntityClasses : found StatefulController, class = \"{}\"", clazz.getName());

				controllerMapping.put(bfName, clazz);
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
				logger.debug("mapControllerAndEntityClasses : mapped \"{}\" to repo \"{}\", beanId=\"{}\"", entityType.getName(), value, bfName);
				entityMapping.put(entityType, bfName);
			}
		}
	}
	
	private void buildFramework(
			String controllerBeanId, 
			Class<?> clazz, 
			BeanDefinitionRegistry reg, 
			Map<Class<?>, String> entityMappings) throws CannotCompileException, IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		
		// Determine the managed class
		// 
		Class<?> statefulClass = clazz.getAnnotation(StatefulController.class).clazz();
		
		// Gather all the events from across all the methods
		//
		Map<String, Map<String, Method>> providersMappings = new HashMap<String, Map<String, Method>>();
		Map<Transition, Method> transitionMapping = new HashMap<Transition, Method>();
		Map<Transition, Method> anyMapping = new HashMap<Transition, Method>();
		Set<String> states = new HashSet<String>();

		// Map the Events and Transitions for the Controller
		//
		mapEventsTransitionsAndStates(clazz, providersMappings, transitionMapping, anyMapping, states);
		
		for(Entry<String, Map<String, Method>> entry : providersMappings.entrySet()) {
			
			// Fetch the binder
			//
			EndpointBinder binder = this.binders.get(entry.getKey());

			// Gather all the events.  When building the proxy, each event
			// will result in a RequestMapping.  The method signature of the RequestMapping
			// will be the same as the declared execution handler minus the first two parameters (Object, event)
			//
			Class<?> proxyClass = binder.bindEndpoints(clazz, entry.getValue(), referenceFactory);

			// Add the new Class to the Bean Registry
			//
			BeanDefinition def = BeanDefinitionBuilder
					.genericBeanDefinition(proxyClass)
					.getBeanDefinition();
			String mvcProxyid = Introspector.decapitalize(clazz.getSimpleName() + MVC_SUFFIX);
			reg.registerBeanDefinition(mvcProxyid, def);
		}
		
		// -- Build the FSM infrastructure --
		
		// Build out a set of States
		//
		List<RuntimeBeanReference> stateBeans = new ManagedList<RuntimeBeanReference>();
		for(String state : states) {
			logger.debug("wireFSM : Registering state \"{}\"", state);
			String stateId = stateBeanId(clazz, state);
			BeanDefinition stateBean = BeanDefinitionBuilder
					.genericBeanDefinition(StateImpl.class)
					.getBeanDefinition();
			stateBean.getPropertyValues().add("name", state);
			reg.registerBeanDefinition(stateId, stateBean);
			stateBeans.add(new RuntimeBeanReference(stateId));
		}
		
		// Build out the Action classes and the Transitions
		//
		RuntimeBeanReference controllerRef = new RuntimeBeanReference(controllerBeanId);
		int cnt = 1;
		for(Entry<Transition, Method> entry : transitionMapping.entrySet()) {
			registerActionAndTransition(
					clazz, 
					entry.getKey().from(), 
					entry.getKey().to(), 
					entry.getKey(), 
					entry.getValue(), 
					controllerRef, 
					cnt, 
					reg);
			cnt++;
		}
		for(Entry<Transition, Method> entry : anyMapping.entrySet()) {
			for (String state : states) {
				registerActionAndTransition(
						clazz, 
						state, 
						(entry.getKey().to().equals(Transition.ANY_STATE)) ? state : entry.getKey().to(), 
						entry.getKey(), 
						entry.getValue(), 
						controllerRef, 
						cnt, 
						reg);
				cnt++;
			}
		}
		
		// Build the Persister
		//
		String startStateId = stateBeanId(
				clazz, 
				clazz.getAnnotation(StatefulController.class).startState());
		
		String persistenceSupportId = null;
		String repoFactoryBeanId = entityMappings.get(statefulClass);
		if (repoFactoryBeanId != null) {
			
			BeanDefinition repoFactoryBean = reg.getBeanDefinition(repoFactoryBeanId);
			Class<?> repoFactoryBeanClass = Class.forName(repoFactoryBean.getBeanClassName());
			
			// Create the PersistenceSupport Bean and map it
			//
			PersistenceSupportBeanFactory factory = this.persistenceFactories.get(repoFactoryBeanClass);

			if (factory != null) {
				
				// Create PersistenceSupport
				//
				persistenceSupportId = factory.registerPersistenceSupport(
						statefulClass, 
						clazz,
						startStateId, 
						stateBeans, 
						repoFactoryBeanId,
						reg);

				// Build the FSM
				//
				String fsmId = Introspector.decapitalize(clazz.getSimpleName() + FSM_SUFFIX);
				BeanDefinition fsmBean = BeanDefinitionBuilder
						.genericBeanDefinition(FSM.class)
						.getBeanDefinition();
				ConstructorArgumentValues args = fsmBean.getConstructorArgumentValues();
				args.addIndexedArgumentValue(0, fsmId);
				args.addIndexedArgumentValue(1, new RuntimeBeanReference(persistenceSupportId));
				reg.registerBeanDefinition(fsmId, fsmBean);

				// Build the FSMHarness
				//
				factory.registerHarness(
						statefulClass, 
						clazz,
						fsmId, 
						persistenceSupportId,
						reg);

			} else {
				logger.warn("mapControllerAndEntityClasses : Unable to locate PersistenceSupportFactory for {} ", clazz.getName());
			}
		}
		
	}
	
	private void registerActionAndTransition(
			Class<?> clazz, 
			String from, 
			String to, 
			Transition transition, 
			Method method, 
			RuntimeBeanReference controllerRef, int cnt, BeanDefinitionRegistry reg) {
		
		logger.debug(
				"registerActionAndTransition : {}:{}->{}:{}",
				from,
				transition.event(),
				to,
				(method == null) ? "noop" : method.getName());
		
		// Build the Action Bean
		//
		RuntimeBeanReference actionRef = null;
		if (method != null) {
			String actionId = Introspector.decapitalize(clazz.getSimpleName() + ".action." + method.getName());
			if (!reg.isBeanNameInUse(actionId)) {
				BeanDefinition actionBean = BeanDefinitionBuilder
						.genericBeanDefinition(MethodInvocationAction.class)
						.getBeanDefinition();
				MutablePropertyValues props = actionBean.getPropertyValues();
				props.add("controller", controllerRef);
				props.add("method", method.getName());
				props.add("parameters", method.getParameterTypes());
				reg.registerBeanDefinition(actionId, actionBean);
			}
			actionRef = new RuntimeBeanReference(actionId);
		}
		
		// Build the Transition Bean
		//
		String transitionId = Introspector.decapitalize(clazz.getSimpleName() + ".transition." + cnt);
		BeanDefinition transitionBean = BeanDefinitionBuilder
				.genericBeanDefinition(TransitionImpl.class)
				.getBeanDefinition();

		String fromId = stateBeanId(clazz, from);
		String toId = stateBeanId(clazz, to);
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

	private String stateBeanId(Class<?> clazz, String state) {
		return Introspector.decapitalize(clazz.getSimpleName() + ".state." + state);
	}
	
	private void mapEventsTransitionsAndStates(
			Class<?> clazz, 
			Map<String, Map<String, Method>> providerMappings,
			Map<Transition, Method> transitionMapping,
			Map<Transition, Method> anyMapping,
			Set<String> states) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		logger.debug("mapEventsTransitionsAndStates : Mapping events and transitions for {}", clazz);
		
		// Map the NOOP Transitions
		//
		StatefulController ctrlAnnotation = clazz.getAnnotation(StatefulController.class);
		for (Transition transition : ctrlAnnotation.noops()) {
			mapTransition(
					transition, 
					null,
					providerMappings,
					transitionMapping, 
					anyMapping,
					states);
		}
		
		// TODO : As we map the events, we need to make sure that the method signature of all the handlers for the event are the same
		
		for(Method method : clazz.getDeclaredMethods()) {
			
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
				"mapTransition : mapping {}:{}->{}",
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
}
