package org.statefulj.framework;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import org.statefulj.framework.actions.MethodInvocationAction;
import org.statefulj.framework.annotations.StatefulController;
import org.statefulj.framework.annotations.Transition;
import org.statefulj.framework.annotations.Transitions;
import org.statefulj.framework.fsm.FSM;
import org.statefulj.framework.fsm.TransitionImpl;
import org.statefulj.framework.model.EndpointBinder;
import org.statefulj.framework.model.ReferenceFactory;
import org.statefulj.framework.model.impl.JPAFSMHarnessImpl;
import org.statefulj.framework.model.impl.ReferenceFactoryImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.persistence.jpa.JPAPerister;

public class StatefulFactory implements BeanDefinitionRegistryPostProcessor {
	
	Logger logger = LoggerFactory.getLogger(StatefulFactory.class);
	
	private final Pattern provider = Pattern.compile("(([^:]*):)?(.*)");

	public static String MVC_SUFFIX = "MVCProxy";
	public static String FSM_SUFFIX = "FSM";
	public static String FSM_HARNESS_SUFFIX = "FSMHarness";
	public static String STATE_SUFFIX = "State";
	
	private ReferenceFactory referenceFactory = new ReferenceFactoryImpl();
	
	private Map<String, EndpointBinder> binders = new HashMap<String, EndpointBinder>();

	public void postProcessBeanFactory(ConfigurableListableBeanFactory reg)
			throws BeansException {
	}

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry reg)
			throws BeansException {
		logger.debug("postProcessBeanDefinitionRegistry : enter");
		try {
			Reflections reflections = new Reflections("org.statefulj");
			Set<Class<? extends EndpointBinder>> subTypes = reflections.getSubTypesOf(EndpointBinder.class);
			for(Class<?> binderClass : subTypes) {
				if (!Modifier.isAbstract(binderClass.getModifiers())) {
					EndpointBinder binder = (EndpointBinder)binderClass.newInstance();
					binders.put(binder.getKey(), binder);
				}
			}
		    
			Map<String, Class<?>> contollerMap = mapControllerClasses(reg);
			for (Entry<String, Class<?>> entry : contollerMap.entrySet()) {
				wireFSM(entry.getKey(), entry.getValue(), reg);
			}
		} catch(Exception e) {
			throw new BeanCreationException("Unable to create bean", e);
		}
		logger.debug("postProcessBeanDefinitionRegistry : exit");
	}
	
	private Map<String, Class<?>> mapControllerClasses(BeanDefinitionRegistry reg) throws ClassNotFoundException {
		Map<String, Class<?>> controllers = new HashMap<String, Class<?>>();
		for(String bfName : reg.getBeanDefinitionNames()) {
			BeanDefinition bf = reg.getBeanDefinition(bfName);
			Class<?> clazz = Class.forName(bf.getBeanClassName());
			if (clazz.isAnnotationPresent(StatefulController.class)) {
				controllers.put(bfName, clazz);
			}
		}
		return controllers;
	}
	
	private void wireFSM(String controllerBeanId, Class<?> clazz, BeanDefinitionRegistry reg) throws CannotCompileException, IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException {
		
		// Determine the managed class
		//
		Class<?> managedClass = clazz.getAnnotation(StatefulController.class).clazz();
		
		// Gather all the events from across all the methods to build the canonical set
		//
//		Map<String, Method> eventMapping = new HashMap<String, Method>();
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
		// TODO : Handle the "Any" case
		RuntimeBeanReference controllerRef = new RuntimeBeanReference(controllerBeanId); // Reference to the controller
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
						state, 
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
		
		String persisterId = Introspector.decapitalize(clazz.getSimpleName() + ".persister");
		BeanDefinition persisterBean = BeanDefinitionBuilder
				.genericBeanDefinition(JPAPerister.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, stateBeans);
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(startStateId));
		args.addIndexedArgumentValue(2, managedClass);
		reg.registerBeanDefinition(persisterId, persisterBean);

		// Build the FSM
		//
		String fsmId = Introspector.decapitalize(clazz.getSimpleName() + FSM_SUFFIX);
		BeanDefinition fsmBean = BeanDefinitionBuilder
				.genericBeanDefinition(FSM.class)
				.getBeanDefinition();
		args = fsmBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(persisterId));
		reg.registerBeanDefinition(fsmId, fsmBean);

		// Build the FSMHarness
		//
		String fsmHarnessId = Introspector.decapitalize(clazz.getSimpleName() + FSM_HARNESS_SUFFIX);
		BeanDefinition fsmHarness = BeanDefinitionBuilder
				.genericBeanDefinition(JPAFSMHarnessImpl.class)
				.getBeanDefinition();
		args = fsmHarness.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmId));
		args.addIndexedArgumentValue(1, managedClass);
		reg.registerBeanDefinition(fsmHarnessId, fsmHarness);
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
				method.getName());
		
		// Build the Action Bean
		//
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
		args.addIndexedArgumentValue(3, new RuntimeBeanReference(actionId));
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
		
		// TODO: As we map the events, we need to make sure that the method signature of all the handlers for the event are the same
		
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
	
	public void mapTransition(
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
		Matcher matcher = this.provider.matcher(event);
		if (!matcher.matches()) {
			throw new RuntimeException("Unable to parse event=" + event);
		}
		return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
	}
}
