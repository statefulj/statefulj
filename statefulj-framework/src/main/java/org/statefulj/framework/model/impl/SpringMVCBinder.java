package org.statefulj.framework.model.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.statefulj.framework.annotations.Transition;
import org.statefulj.framework.annotations.Transitions;
import org.statefulj.framework.model.EndpointBinder;
import org.statefulj.framework.model.FSMHarness;
import org.statefulj.framework.model.ReferenceFactory;

public class SpringMVCBinder implements EndpointBinder {
	
	public final static String KEY = "springmvc";

	private Logger logger = LoggerFactory.getLogger(SpringMVCBinder.class);
	
	private final Pattern methodPattern = Pattern.compile("(([^:]*):)?(.*)");

	public static String MVC_SUFFIX = "MVCProxy";
	public static String FSM_SUFFIX = "FSM";
	public static String FSM_HARNESS_SUFFIX = "FSMHarness";
	public static String STATE_SUFFIX = "State";
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public Class<?> bindEndpoints(
			Class<?> clazz,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory)
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		logger.debug("bindEndpoints : Building proxy for {}", clazz);
		
		// Set up the ClassPool
		//
		ClassPool cp = ClassPool.getDefault();
		cp.appendClassPath(new ClassClassPath(getClass()));

		// Create a new Proxy Class 
		//
		String mvcProxyClassName = clazz.getName() + MVC_SUFFIX;
		CtClass mvcProxyClass = cp.makeClass(mvcProxyClassName);
		
		// Add the SpringMVC Controller annotation to the Proxy
		//
		addControllerAnnotation(mvcProxyClass);
		
		// Add the member variable referencing the StatefulController
		//
		addHarnessReference(mvcProxyClass, refFactory.fsmHarness(clazz.getSimpleName()), cp);
		
		// Copy methods from bean to the new proxy class
		//
		addMethods(mvcProxyClass, eventMapping, cp);
		
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
	
	private void addHarnessReference(CtClass mvcProxyClass, String fsmHarnessId, ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(FSMHarness.class.getName());
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

	
	private void addMethods(CtClass mvcProxyClass, Map<String,Method> eventMapping, ClassPool cp) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// Build a method for each Event
		//
		for(String event : eventMapping.keySet()) {
			addMethod(mvcProxyClass, event, eventMapping.get(event), cp);
		}
	}
	
	private void addMethod(CtClass mvcProxyClass, String event, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		Pair<String, String> methodEndpoint = this.parseMethod(event);
		String requestMethod = methodEndpoint.getLeft();
		String requestEvent = methodEndpoint.getRight();
		requestMethod = (requestMethod == null) ? RequestMethod.GET.toString() : requestMethod.toUpperCase();

		
		// References id?
		//
		boolean referencesId = (event.indexOf("{id}") > 0);

		// Clone Method from the StatefulController
		//
		CtMethod ctMethod = createMethod(mvcProxyClass, requestMethod, requestEvent, method, cp);

		// TODO : Validate that the method begins with : Stateful, event
		
		// Clone method Annotations
		//
		addMethodAnnotations(ctMethod, method);

		// Add a RequestMapping annotation
		//
		addRequestMapping(ctMethod, requestMethod, requestEvent);

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
	
	private CtMethod createMethod(
			CtClass mvcProxyClass, 
			String requestMethod, 
			String requestEvent, 
			Method method, 
			ClassPool cp) throws NotFoundException {
		String methodName = ("$_" + requestMethod + requestEvent.replace("/", "_").replace("{", "").replace("}", "")).toLowerCase();

		logger.debug(
				"createMethod : Create method {} for {}", 
				methodName,
				mvcProxyClass.getSimpleName());

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
	
	private void addRequestMapping(CtMethod ctMethod, String method, String request) {
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();

		AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation requestMapping = new Annotation(RequestMapping.class.getName(), constPool);
		
		ArrayMemberValue valueVals = new ArrayMemberValue(constPool);
		StringMemberValue valueVal = new StringMemberValue(constPool);
		valueVal.setValue(request);
		valueVals.setValue(new MemberValue[]{valueVal});
		
		requestMapping.addMemberValue("value", valueVals);
		
		// TODO : Parse the protocol from the url
		//
		ArrayMemberValue methodVals = new ArrayMemberValue(constPool);
		EnumMemberValue methodVal = new EnumMemberValue(constPool);
		methodVal.setType(RequestMethod.class.getName());
		methodVal.setValue(method);
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
		logger.debug("createAnnotation : Create annotation = {}", annotation.annotationType().getName());
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

	private Pair<String, String> parseMethod(String event) {
		Matcher matcher = this.methodPattern.matcher(event);
		if (!matcher.matches()) {
			throw new RuntimeException("Unable to parse event=" + event);
		}
		return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
	}
}
