package org.statefulj.framework.binders.common;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.addResourceAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.statefulj.framework.core.model.FSMHarness;

public abstract class AbstractRestfulBinder {

	private Logger logger = LoggerFactory.getLogger(AbstractRestfulBinder.class);
	
	private final String HARNESS_VAR = "harness";
	private final String CONTROLLER_VAR = "controller";
	
	private LocalVariableTableParameterNameDiscoverer parmDiscover = new LocalVariableTableParameterNameDiscoverer();

	protected CtMethod createRequestMethod(
			CtClass mvcProxyClass, 
			String requestMethod, 
			String requestEvent, 
			Method method, 
			ClassPool cp) throws NotFoundException {
		String methodName = ("$_" + requestMethod + requestEvent.replace("/", "_").replace("{", "").replace("}", "")).toLowerCase();

		logger.debug(
				"Create method {} for {}", 
				methodName,
				mvcProxyClass.getSimpleName());

		CtClass returnClass = (method == null) ? CtClass.voidType : cp.get(method.getReturnType().getName());
		CtMethod ctMethod = new CtMethod(returnClass, methodName, null, mvcProxyClass);
		return ctMethod;
	}
	
	protected void addRequestParameters(
			boolean referencesId, 
			CtMethod ctMethod, 
			Method method, 
			ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

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
			int annotationCnt = (referencesId) 
					? method.getParameterTypes().length 
					: method.getParameterTypes().length - 1;
			annotationCnt = Math.max(annotationCnt, 1);

			// Pull the Parameter Annotations from the StatefulController - we're going to skip
			// over the first two - but then we're going to add a parameter for the HttpServletRequest and "id" parameter
			//
			java.lang.annotation.Annotation[][] parmAnnotations = method.getParameterAnnotations();
			paramArrays = new Annotation[annotationCnt][];
			
			// Add an Id parameter at the beginning of the method - this will be 
			// used by the Harness to fetch the object 
			//
			if (referencesId) {
				paramArrays[parmIndex] = addIdParameter(ctMethod, cp);
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
				if (parmCnt < 2) {
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
			paramArrays[0] = addIdParameter(ctMethod, cp);
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
		
		return new Annotation[] {
				new Annotation(
					ctMethod.getMethodInfo().getConstPool(), 
					cp.getCtClass(Context.class.getName())
				) };
		
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
	
	protected void addProxyMethodBody(CtMethod ctMethod, Method method) throws CannotCompileException, NotFoundException {
		String returnType = ctMethod.getReturnType().getName();
		
		String returnStmt = 
				(returnType.equals("void")) 
				? ""
				: "return (" + returnType + ")";
		
		String methodBody = "{ " 
				+ returnStmt
				+ "$proceed($$); }";

		ctMethod.setBody(methodBody, "this." + CONTROLLER_VAR, method.getName());
	}
	
	protected void addControllerReference(
			CtClass mvcProxyClass,
			Class<?> clazz,
			String beanName, 
			ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(clazz.getName());
		CtField field = new CtField(type, CONTROLLER_VAR, mvcProxyClass);

		addResourceAnnotation(field, beanName);
		
		mvcProxyClass.addField(field);
	}

	protected void addFSMHarnessReference(CtClass mvcProxyClass, String fsmHarnessId, ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(FSMHarness.class.getName());
		CtField field = new CtField(type, HARNESS_VAR, mvcProxyClass);

		addResourceAnnotation(field, fsmHarnessId);
		
		mvcProxyClass.addField(field);
	}

	protected abstract Annotation[] addIdParameter(CtMethod ctMethod, ClassPool cp) throws NotFoundException, CannotCompileException;
	
	protected abstract Annotation[] createParameterAnnotations(
			String parmName, 
			MethodInfo methodInfo, 
			java.lang.annotation.Annotation[] annotations, 
			ConstPool parameterConstPool) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;
	
	protected abstract Pattern getMethodPattern();
}
