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
package org.statefulj.framework.binders.common;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.addMethodAnnotations;
import static org.statefulj.framework.binders.common.utils.JavassistUtils.addResourceAnnotation;
import static org.statefulj.framework.binders.common.utils.JavassistUtils.cloneAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.ReferenceFactory;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.*;

public abstract class AbstractRestfulBinder implements EndpointBinder {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRestfulBinder.class);
	
	private final Pattern methodPattern = Pattern.compile("(([^:]*):)?(.*)");
	
	private final String HARNESS_VAR = "harness";
	private final String GET = "GET";
	
	private LocalVariableTableParameterNameDiscoverer parmDiscover = new LocalVariableTableParameterNameDiscoverer();

	@Override
	public Class<?> bindEndpoints(
			String beanName, 
			Class<?> statefulControllerClass,
			Class<?> idType,
			boolean isDomainEntity,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory)
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		
		logger.debug("Building proxy for {}", statefulControllerClass);
		
		// Set up the ClassPool
		//
		ClassPool cp = ClassPool.getDefault();
		cp.appendClassPath(new ClassClassPath(getClass()));

		// Create a new Proxy Class 
		//
		String proxyClassName = statefulControllerClass.getName() + getSuffix();
		
		// Construct and return the Proxy Class
		//
		return buildProxy(
				cp,
				beanName, 
				proxyClassName,
				statefulControllerClass,
				idType,
				isDomainEntity,
				eventMapping, 
				refFactory).toClass();
	}
	
	protected CtClass buildProxy(
			ClassPool cp,
			String beanName, 
			String proxyClassName,
			Class<?> statefulControllerClass,
			Class<?> idType,
			boolean isDomainEntity,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory) 
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		
		CtClass proxyClass = cp.makeClass(proxyClassName);
		
		// Add the Component annotation to the Proxy
		//
		addComponentAnnotation(proxyClass);
		
		// Add the member variable referencing the FSMHarness
		//
		addFSMHarnessReference(proxyClass, refFactory.getFSMHarnessId(), cp);
		
		// Copy methods that have a Transition annotation from the StatefulController to the Binder
		//
		addRequestMethods(proxyClass, idType, isDomainEntity, eventMapping, cp);
		
		return proxyClass;
	}
	
	protected void addComponentAnnotation(CtClass proxyClass) {
		addClassAnnotation(proxyClass, getComponentClass());
	}
	
	protected void addRequestMethods(
			CtClass proxyClass, 
			Class<?> idType,
			boolean isDomainEntity,
			Map<String,Method> eventMapping, 
			ClassPool cp) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// Build a method for each Event
		//
		for(String event : eventMapping.keySet()) {
			addRequestMethod(
					proxyClass,
					idType,
					isDomainEntity,
					event, 
					eventMapping.get(event), 
					cp);
		}
	}
	
	protected CtMethod createRequestMethod(
			CtClass proxyClass, 
			String requestMethod, 
			String requestEvent, 
			Method method, 
			ClassPool cp) throws NotFoundException {
		String methodName = ("$_" + requestMethod + requestEvent.replace("/", "_").replace("{", "").replace("}", "")).toLowerCase();

		logger.debug(
				"Create method {} for {}", 
				methodName,
				proxyClass.getSimpleName());

		CtClass returnClass = (method == null) ? CtClass.voidType : cp.get(method.getReturnType().getName());
		CtMethod ctMethod = new CtMethod(returnClass, methodName, null, proxyClass);
		return ctMethod;
	}
	
	protected void addRequestParameters(
			boolean referencesId, 
			Class<?> idType,
			boolean isDomainEntity,
			CtMethod ctMethod, 
			Method method, 
			ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		int fixedParmCnt = (isDomainEntity) ? 1 : 2;
		
		String[] parmNames = (method != null) ? parmDiscover.getParameterNames(method) : null;
 		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ParameterAnnotationsAttribute paramAtrributeInfo = 
				new ParameterAnnotationsAttribute(
						methodInfo.getConstPool(), 
						ParameterAnnotationsAttribute.visibleTag);
		
		Annotation[][] paramArrays = null;
		if (method != null) {
			
			int parmIndex = 0;
			
			// Does this event reference the stateful object?
			//
			int additionalParmCnt = (referencesId) ? 2 : 1;
			int annotationCnt = method.getParameterTypes().length + additionalParmCnt - fixedParmCnt;
			annotationCnt = Math.max(annotationCnt, additionalParmCnt);

			// Pull the Parameter Annotations from the StatefulController - we're going to skip
			// over the first one (DomainEntity) or two (Controller) - but then we're going to 
			// add a parameter for the HttpServletRequest and "id" parameter
			//
			java.lang.annotation.Annotation[][] parmAnnotations = method.getParameterAnnotations();
			paramArrays = new Annotation[annotationCnt][];
			
			// Add an Id parameter at the beginning of the method - this will be 
			// used by the Harness to fetch the object 
			//
			if (referencesId) {
				paramArrays[parmIndex] = addIdParameter(ctMethod, idType, cp);
				parmIndex++;
			}
			
			// Add an HttpServletRequest - this will be passed in as a context to the finder/factory methods
			//
			paramArrays[parmIndex] = addHttpRequestParameter(ctMethod, cp);
			parmIndex++;
			
			int parmCnt = 0;
			for(Class<?> parm : method.getParameterTypes()) {
				
				// Skip first two parameters - they are the Stateful Object and event String.
				//
				if (parmCnt < fixedParmCnt) {
					parmCnt++;
					continue;
				}
				
				// Clone the parameter Class
				//
				CtClass ctParm = cp.get(parm.getName());
				
				// Add the parameter to the method
				//
				ctMethod.addParameter(ctParm);
				
				// Add the Parameter Annotations to the Method
				//
				String parmName = (parmNames != null && parmNames.length > parmCnt) ? parmNames[parmCnt] : null;
				paramArrays[parmIndex] = createParameterAnnotations(
						parmName,
						ctMethod.getMethodInfo(),
						parmAnnotations[parmCnt],
						paramAtrributeInfo.getConstPool());
				parmCnt++;
				parmIndex++;
			}
		} else {
			// NOOP transitions always a require an object Id
			//
			paramArrays = new Annotation[2][];
			paramArrays[0] = addIdParameter(ctMethod, idType, cp);
			paramArrays[1] = addHttpRequestParameter(ctMethod, cp);
		}
		paramAtrributeInfo.setAnnotations(paramArrays);
		methodInfo.addAttribute(paramAtrributeInfo);
	}
	
	protected void copyParameters(CtMethod ctMethod, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		String[] parmNames = (method != null) ? parmDiscover.getParameterNames(method) : null;
 		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ParameterAnnotationsAttribute paramAtrributeInfo = 
				new ParameterAnnotationsAttribute(
						methodInfo.getConstPool(), 
						ParameterAnnotationsAttribute.visibleTag);
		
		Annotation[][] paramArrays = new Annotation[method.getParameterTypes().length][];
		java.lang.annotation.Annotation[][] parmAnnotations = method.getParameterAnnotations();
		int parmIndex = 0;
		for(Class<?> parm : method.getParameterTypes()) {
				
			// Clone the parameter Class
			//
			CtClass ctParm = cp.get(parm.getName());
			
			// Add the parameter to the method
			//
			ctMethod.addParameter(ctParm);
			
			// Add the Parameter Annotations to the Method
			//
			String parmName = (parmNames != null && parmNames.length > parmIndex) ? parmNames[parmIndex] : null;
			paramArrays[parmIndex] = createParameterAnnotations(
					parmName,
					ctMethod.getMethodInfo(),
					parmAnnotations[parmIndex],
					paramAtrributeInfo.getConstPool());
			parmIndex++;
		}
		paramAtrributeInfo.setAnnotations(paramArrays);
		methodInfo.addAttribute(paramAtrributeInfo);
	}
	
	protected Annotation[] addHttpRequestParameter(CtMethod ctMethod, ClassPool cp) throws NotFoundException, CannotCompileException {
		// Map the HttpServletRequest class
		//
		CtClass ctParm = cp.get(HttpServletRequest.class.getName());
		
		// Add the parameter to the method
		//
		ctMethod.addParameter(ctParm);
		
		return new Annotation[] {};
		
	}
	
	protected Pair<String, String> parseMethod(String event) {
		Matcher matcher = getMethodPattern().matcher(event);
		if (!matcher.matches()) {
			throw new RuntimeException("Unable to parse event=" + event);
		}
		return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
	}

	protected void addRequestMethodBody(boolean referencesId, CtMethod ctMethod, String event) throws CannotCompileException, NotFoundException {
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

		ctMethod.setBody(methodBody, "this." + HARNESS_VAR, "onEvent");
	}
	
	protected void addFSMHarnessReference(CtClass proxyClass, String fsmHarnessId, ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(FSMHarness.class.getName());
		CtField field = new CtField(type, HARNESS_VAR, proxyClass);

		addResourceAnnotation(field, fsmHarnessId);
		
		proxyClass.addField(field);
	}

	protected void addRequestMethod(
			CtClass proxyClass,
			Class<?> idType,
			boolean isDomainEntity,
			String event, 
			Method method, 
			ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		Pair<String, String> methodEndpoint = this.parseMethod(event);
		String requestMethod = methodEndpoint.getLeft();
		String requestEvent = methodEndpoint.getRight();
		requestMethod = (requestMethod == null) ? GET : requestMethod.toUpperCase();

		
		// References id?
		//
		boolean referencesId = (event.indexOf("{id}") > 0);

		// Clone Method from the StatefulController
		//
		CtMethod ctMethod = createRequestMethod(proxyClass, requestMethod, requestEvent, method, cp);

		// Clone method Annotations
		//
		addMethodAnnotations(ctMethod, method);

		// Add a Endpoint mapping
		//
		addEndpointMapping(ctMethod, requestMethod, requestEvent);

		// Clone the parameters, along with the Annotations
		//
		addRequestParameters(referencesId, idType, isDomainEntity, ctMethod, method, cp);

		// Add the Method Body
		//
		addRequestMethodBody(referencesId, ctMethod, event);
		
		// Add the Method to the Proxy class
		//
		proxyClass.addMethod(ctMethod);
	}

	protected Annotation[] addIdParameter(
			CtMethod ctMethod, 
			Class<?> idType,
			ClassPool cp) throws NotFoundException, CannotCompileException {
		// Clone the parameter Class
		//
		CtClass ctParm = cp.get(idType.getName());
		
		// Add the parameter to the method
		//
		ctMethod.addParameter(ctParm);
		
		// Add the Parameter Annotations to the Method
		//
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();
		Annotation annot = new Annotation(getPathAnnotationClass().getName(), constPool);
		
		StringMemberValue valueVal = new StringMemberValue("id", constPool); 
		annot.addMemberValue("value", valueVal);
		
		return new Annotation[]{ annot };
	}

	protected Annotation[] createParameterAnnotations(String parmName,
			MethodInfo methodInfo,
			java.lang.annotation.Annotation[] annotations,
			ConstPool parameterConstPool) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		List<Annotation> ctParmAnnotations = new LinkedList<Annotation>();

		for(java.lang.annotation.Annotation annotation : annotations) {
			Annotation clone = cloneAnnotation(parameterConstPool, annotation);
			ctParmAnnotations.add(clone);
		}
		return ctParmAnnotations.toArray(new Annotation[]{});
	}

	protected Pattern getMethodPattern() {
		return methodPattern;
	}

	protected Class<?> getComponentClass() {
		return Component.class;
	}
	
	protected abstract void addEndpointMapping(CtMethod ctMethod, String method, String request);

	protected abstract Class<?> getPathAnnotationClass();

	protected abstract String getSuffix();
	
}
