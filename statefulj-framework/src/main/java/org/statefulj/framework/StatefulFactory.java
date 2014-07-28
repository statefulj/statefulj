package org.statefulj.framework;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.statefulj.framework.actions.MethodInvocationAction;
import org.statefulj.framework.annotations.StatefulController;
import org.statefulj.framework.annotations.Transition;
import org.statefulj.framework.annotations.Transitions;
import org.statefulj.fsm.FSM;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.impl.DeterministicTransitionImpl;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.persistence.jpa.JPAPerister;

public class StatefulFactory implements BeanDefinitionRegistryPostProcessor {
	
	Logger logger = LoggerFactory.getLogger(StatefulFactory.class);

	public static String MVC_SUFFIX = "MVCProxy";
	public static String FSM_SUFFIX = "FSM";
	public static String FSM_HARNESS_SUFFIX = "FSMHarness";
	public static String State_SUFFIX = "State";

	public void postProcessBeanFactory(ConfigurableListableBeanFactory reg)
			throws BeansException {
	}

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry reg)
			throws BeansException {
		logger.debug("postProcessBeanDefinitionRegistry : enter");
		try {
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
		Map<String, Method> eventMapping = new HashMap<String, Method>();
		Map<Transition, Method> transitionMapping = new HashMap<Transition, Method>();
		Set<String> states = new HashSet<String>();
		
		// Gather all the events.  When building the proxy, each event
		// will result in a RequestMapping.  The method signature of the RequestMapping
		// will be the same as the declared execution handler
		//
		Class<?> proxyClass = buildMVCProxy(clazz, eventMapping, transitionMapping, states, reg);

		// Add the new Class to the Bean Registry
		//
		BeanDefinition def = BeanDefinitionBuilder
				.genericBeanDefinition(proxyClass)
				.getBeanDefinition();
		String mvcProxyid = Introspector.decapitalize(clazz.getSimpleName() + MVC_SUFFIX);
		reg.registerBeanDefinition(mvcProxyid, def);
		
		// Build the FSM infrastructure in the parent
		// Build out a set of States
		//
		List<RuntimeBeanReference> stateBeans = new ManagedList<RuntimeBeanReference>();
		for(String state : states) {
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

			// Build the Action Bean
			//
			String actionId = Introspector.decapitalize(clazz.getSimpleName() + ".action." + entry.getValue().getName());
			if (!reg.isBeanNameInUse(actionId)) {
				BeanDefinition actionBean = BeanDefinitionBuilder
						.genericBeanDefinition(MethodInvocationAction.class)
						.getBeanDefinition();
				MutablePropertyValues props = actionBean.getPropertyValues();
				props.add("controller", controllerRef);
				props.add("method", entry.getValue().getName());
				props.add("parameters", entry.getValue().getParameterTypes());
				reg.registerBeanDefinition(actionId, actionBean);
			}
			
			// Build the Transition Bean
			//
			String transitionId = Introspector.decapitalize(clazz.getSimpleName() + ".transition." + cnt);
			BeanDefinition transitionBean = BeanDefinitionBuilder
					.genericBeanDefinition(DeterministicTransitionImpl.class)
					.getBeanDefinition();
			String fromId = stateBeanId(clazz, entry.getKey().from());
			String toId = stateBeanId(clazz, entry.getKey().to());

			ConstructorArgumentValues args = transitionBean.getConstructorArgumentValues();
			args.addIndexedArgumentValue(0, new RuntimeBeanReference(fromId));
			args.addIndexedArgumentValue(1, new RuntimeBeanReference(toId));
			args.addIndexedArgumentValue(2, entry.getKey().event());
			args.addIndexedArgumentValue(3, new RuntimeBeanReference(actionId));
			
			reg.registerBeanDefinition(transitionId, transitionBean);
			cnt++;
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
				.genericBeanDefinition(JPAFSMHarness.class)
				.getBeanDefinition();
		args = fsmHarness.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmId));
		args.addIndexedArgumentValue(1, managedClass);
		reg.registerBeanDefinition(fsmHarnessId, fsmHarness);
	}
	
	private String stateBeanId(Class<?> clazz, String state) {
		return Introspector.decapitalize(clazz.getSimpleName() + ".state." + state);
	}
	
	private Class<?> buildMVCProxy(
			Class<?> clazz, 
			Map<String, Method> eventMapping, 
			Map<Transition, Method> transitionMapping, 
			Set<String> states, 
			BeanDefinitionRegistry reg) throws CannotCompileException, NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		
		// Map the Events for the Class
		//
		mapEventsTransitionsAndStates(clazz, eventMapping, transitionMapping, states);

		// Set up the ClassPool
		//
		ClassPool cp = ClassPool.getDefault();
		cp.appendClassPath(new ClassClassPath(clazz));

		// Create a new Proxy Class 
		//
		String mvcProxyClassName = clazz.getName() + MVC_SUFFIX;
		CtClass mvcProxyClass = cp.makeClass(mvcProxyClassName);
		
		// Add the SpringMVC Controller annotation to the Proxy
		//
		addControllerAnnotation(mvcProxyClass);
		
		// Add the member variable referencing the StatefulController
		//
		addHarnessReference(mvcProxyClass, clazz, cp);
		
		// Copy methods from bean to the new proxy class
		//
		addMethods(mvcProxyClass, eventMapping, clazz, cp);
		
		// Construct and return the Proxy Class
		//
		return mvcProxyClass.toClass();
	}
	
	private void addControllerAnnotation(CtClass mvcProxyClass) {
		ClassFile ccFile = mvcProxyClass.getClassFile();
		ConstPool constPool = ccFile.getConstPool();
		AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation annot = new Annotation(Controller.class.getName(), constPool);
		attr.addAnnotation(annot);
		ccFile.addAttribute(attr);
	}
	
	private void addHarnessReference(CtClass mvcProxyClass, Class<?> controller, ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(JPAFSMHarness.class.getName());
		String fsmHarnessId = Introspector.decapitalize(controller.getSimpleName() + FSM_HARNESS_SUFFIX);
		CtField field = new CtField(type, "harness", mvcProxyClass);
		FieldInfo fi = field.getFieldInfo();
		
		AnnotationsAttribute attr = new AnnotationsAttribute(
				field.getFieldInfo().getConstPool(), 
				AnnotationsAttribute.visibleTag);
		Annotation annot = new Annotation(Resource.class.getName(), fi.getConstPool());
		
		StringMemberValue nameValue = new StringMemberValue(fi.getConstPool());
		nameValue.setValue(fsmHarnessId);
		annot.addMemberValue("name", nameValue);
		
		attr.addAnnotation(annot);
		fi.addAttribute(attr);
		mvcProxyClass.addField(field);
	}

	private void mapEventsTransitionsAndStates(
			Class<?> clazz, 
			Map<String, Method> eventMapping, 
			Map<Transition, Method> transitionMapping,
			Set<String> states) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// TODO: As we map the events, we need to make sure that the method signature of all the handlers for the event are the same
		
		for(Method method : clazz.getDeclaredMethods()) {
			Transitions transitions = method.getAnnotation(Transitions.class);
			if (transitions != null) {
				for(Transition transition : transitions.value()) {
					mapTransition(
							transition, 
							method,
							eventMapping, 
							transitionMapping, 
							states);
				}
			}
			Transition transition = method.getAnnotation(Transition.class);
			if (transition != null) {
				mapTransition(
						transition, 
						method,
						eventMapping, 
						transitionMapping, 
						states);
			}
		}
	}
	
	public void mapTransition(
			Transition transition, 
			Method method,
			Map<String, Method> eventMapping, 
			Map<Transition, Method> transitionMapping,
			Set<String> states) {
		
		eventMapping.put(transition.event(), method);
		transitionMapping.put(transition, method);
		if (!transition.from().equals(State.ANY_STATE)) {
			states.add(transition.from());
		}
		if (!transition.to().equals(State.ANY_STATE)) {
			states.add(transition.to());
		}
	}
	
	private void addMethods(CtClass mvcProxyClass, Map<String,Method> eventMapping, Class<?> clazz, ClassPool cp) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// Build a method for each Event
		//
		for(String event : eventMapping.keySet()) {
			addMethod(mvcProxyClass, event, eventMapping.get(event), cp);
		}
	}
	
	private void addMethod(CtClass mvcProxyClass, String event, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		// References id?
		//
		boolean referencesId = (event.indexOf("{id}") > 0);

		// Clone Method from the StatefulController
		//
		CtMethod ctMethod = createMethod(mvcProxyClass, event, method, cp);

		// TODO : Validate that the method begins with : Stateful, event
		
		// Clone method Annotations
		//
		addMethodAnnotations(ctMethod, method);

		// Add a RequestMapping annotation
		//
		addRequestMapping(ctMethod, event);

		// Clone the parameters, along with the Annotations
		//
		addParameters(referencesId, ctMethod, method, cp);

		// Add the Method Body
		//
		addMethodBody(referencesId, ctMethod, event);
		
		// Add the Method to the Proxy class
		//
		mvcProxyClass.addMethod(ctMethod);
	}
	
	private CtMethod createMethod(CtClass mvcProxyClass, String event, Method method, ClassPool cp) throws NotFoundException {
		String methodName = "$" + event.replace("/", "_").replace("{", "").replace("}", "").toLowerCase();
		CtMethod ctMethod = new CtMethod(cp.getCtClass(method.getReturnType().getName()), methodName, null, mvcProxyClass);
		ctMethod.setModifiers(method.getModifiers());
		return ctMethod;
	}
	
	private void addMethodAnnotations(CtMethod ctMethod, Method method) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();
		for(java.lang.annotation.Annotation anno : method.getAnnotations()) {
			AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

			// If it's a Transition - convert a RequestMapping annotation; otherwise,
			// clone the annotation
			//
			Annotation clone = null;
			if (anno instanceof Transitions || anno instanceof Transition) {
				// skip
			} else {
				clone = createAnnotation(constPool, anno);
				attr.addAnnotation(clone);
				methodInfo.addAttribute(attr);
			}
		}
	}
	
	private void addRequestMapping(CtMethod ctMethod, String event) {
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();

		AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation requestMapping = new Annotation(RequestMapping.class.getName(), constPool);
		
		ArrayMemberValue valueVals = new ArrayMemberValue(constPool);
		StringMemberValue valueVal = new StringMemberValue(constPool);
		valueVal.setValue(event);
		valueVals.setValue(new MemberValue[]{valueVal});
		
		requestMapping.addMemberValue("value", valueVals);
		
		// TODO : Parse the protocol from the url
		//
		ArrayMemberValue methodVals = new ArrayMemberValue(constPool);
		EnumMemberValue methodVal = new EnumMemberValue(constPool);
		methodVal.setType(RequestMethod.class.getName());
		methodVal.setValue(RequestMethod.GET.toString());
		methodVals.setValue(new MemberValue[]{methodVal});
		
		requestMapping.addMemberValue("method", methodVals);
		attr.addAnnotation(requestMapping);
		methodInfo.addAttribute(attr);
	}
	
	private void addMethodBody(boolean referencesId, CtMethod ctMethod, String event) throws CannotCompileException, NotFoundException {
		String nullObjId = 
				(referencesId) 
				? "\"" 
				: "\", null";
		
		String returnType = ctMethod.getReturnType().getName();
		
		String returnStmt = 
				(returnType.equals("void")) 
				? ""
				: "return (" + returnType + ")";
		
		String methodBody = "{ " 
				+ returnStmt
				+ "$proceed(\"" 
				+ event 
				+ nullObjId
				+ ", $args); }";

		ctMethod.setBody(methodBody, "this.harness","onEvent");
	}
	
	private void addParameters(boolean referencesId, CtMethod ctMethod, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		int parmIndex = 0;
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ParameterAnnotationsAttribute paramAtrributeInfo = 
				new ParameterAnnotationsAttribute(
						methodInfo.getConstPool(), 
						ParameterAnnotationsAttribute.visibleTag);
		
		// Does this event reference the stateful object?
		//
		int annotationCnt = (referencesId) 
				? method.getParameterTypes().length - 1 
				: method.getParameterTypes().length - 2;

		// Pull the Parameter Annotations from the StatefulController - we're going to skip
		// over the first two - but then we're going to add an "id" parameter
		//
		java.lang.annotation.Annotation[][] parmAnnotations = method.getParameterAnnotations();
		Annotation[][] paramArrays = new Annotation[annotationCnt][];
		
		// Add an Id parameter at the beginning of the method - this will be 
		// used by the Harness to fetch the object 
		//
		if (referencesId) {
			paramArrays[parmIndex] = addIdParameter(ctMethod, cp);
			parmIndex++;
		}
		
		int parmCnt = 0;
		for(Class<?> parm : method.getParameterTypes()) {
			
			// Skip first two parameters - they are the Stateful Object and event String.
			//
			if (parmCnt < 2) {
				parmCnt++;
				continue;
			}
			
			// Clone the parameter Class
			//
			CtClass ctParm = cp.getCtClass(parm.getName());
			
			// Add the parameter to the method
			//
			ctMethod.addParameter(ctParm);
			
			// Add the Parameter Annotations to the Method
			//
			paramArrays[parmIndex] = createParameterAnnotations(
					ctMethod.getMethodInfo(),
					parmAnnotations[parmCnt],
					paramAtrributeInfo.getConstPool());
			parmCnt++;
			parmIndex++;
		}
		paramAtrributeInfo.setAnnotations(paramArrays);
		methodInfo.addAttribute(paramAtrributeInfo);
	}
	
	private Annotation[] addIdParameter(CtMethod ctMethod, ClassPool cp) throws NotFoundException, CannotCompileException {
		// Clone the parameter Class
		//
		CtClass ctParm = cp.getCtClass(Long.class.getName());
		
		// Add the parameter to the method
		//
		ctMethod.addParameter(ctParm);
		
		// Add the Parameter Annotations to the Method
		//
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();
		AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation annot = new Annotation(PathVariable.class.getName(), constPool);
		
		StringMemberValue valueVal = new StringMemberValue("id", constPool); 
		annot.addMemberValue("value", valueVal);
		attr.addAnnotation(annot);
		
		return new Annotation[]{ annot };
	}
	
	/**
	 * Clone all the parameter Annotations from the StatefulController to the Proxy
	 * @param methodInfo
	 * @param parmIndex
	 * @param annotations
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Annotation[] createParameterAnnotations(MethodInfo methodInfo, java.lang.annotation.Annotation[] annotations, ConstPool parameterConstPool) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		List<Annotation> ctParmAnnotations = new LinkedList<Annotation>();

		for(java.lang.annotation.Annotation annotation : annotations) {
			Annotation clone = createAnnotation(parameterConstPool, annotation);
			AnnotationsAttribute attr = new AnnotationsAttribute(parameterConstPool, AnnotationsAttribute.visibleTag);
			attr.addAnnotation(clone);
			ctParmAnnotations.add(clone);
		}
		return ctParmAnnotations.toArray(new Annotation[]{});
	}
	
	/**
	 * Clone an annotation and all of it's methods
	 * @param constPool
	 * @param annotation
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Annotation createAnnotation(ConstPool constPool, java.lang.annotation.Annotation annotation) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<?> clazz = annotation.annotationType();

		Annotation annot = new Annotation(clazz.getName(), constPool);
		for(Method method : clazz.getDeclaredMethods()) {
			MemberValue memberVal = null;
			
			if (method.getReturnType().isArray()) {
				List<MemberValue> memberVals = new LinkedList<MemberValue>();
				for(Object val : (Object[])method.invoke(annotation)) {
					memberVals.add(createMemberValue(constPool, val));
				}
				memberVal = new ArrayMemberValue(constPool);
				((ArrayMemberValue)memberVal).setValue(memberVals.toArray(new MemberValue[]{}));
			} else {
				memberVal = createMemberValue(constPool, method.invoke(annotation));
			}
			annot.addMemberValue(method.getName(), memberVal);
		}
		return annot;
	}
		
	private MemberValue createMemberValue(ConstPool constPool, Object val) {
		MemberValue memberVal = null;
		
		if (val instanceof Boolean) {
			memberVal = new BooleanMemberValue((Boolean)val, constPool);
		}
		else if (val instanceof Byte) {
			memberVal = new ByteMemberValue((Byte)val, constPool);
		}
		else if (val instanceof Character) {
			memberVal = new CharMemberValue((Byte)val, constPool);
		}
		else if (val instanceof Class) {
			memberVal = new ClassMemberValue(((Class<?>)val).getName(), constPool);
		}
		else if (val instanceof Double) {
			memberVal = new DoubleMemberValue((Double)val, constPool);
		}
		else if (val instanceof Float) {
			memberVal = new FloatMemberValue((Float)val, constPool);
		}
		else if (val instanceof Integer) {
			memberVal = new IntegerMemberValue((Integer)val, constPool);
		}
		else if (val instanceof Short) {
			memberVal = new ShortMemberValue((Short)val, constPool);
		}
		else if (val instanceof Long) {
			memberVal = new LongMemberValue((Long)val, constPool);
		}
		else if (val instanceof String) {
			memberVal = new StringMemberValue((String)val, constPool); 
		}
		else if (val instanceof Enum) {
			memberVal = new EnumMemberValue(constPool);
			((EnumMemberValue)memberVal).setType(val.getClass().getName());
			((EnumMemberValue)memberVal).setValue(((Enum<?>)val).toString());
		}
		return memberVal;
	}

}
