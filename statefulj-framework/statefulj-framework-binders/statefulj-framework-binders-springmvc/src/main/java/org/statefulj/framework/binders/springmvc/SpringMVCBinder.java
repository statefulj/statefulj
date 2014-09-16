package org.statefulj.framework.binders.springmvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.*;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.ReferenceFactory;

// TODO : Handle when an action doesn't have either the User or Event parameter
// TODO : Copy over ExceptionHandlers
public class SpringMVCBinder implements EndpointBinder {
	
	public final static String KEY = "springmvc";

	private Logger logger = LoggerFactory.getLogger(SpringMVCBinder.class);
	
	private final Pattern methodPattern = Pattern.compile("(([^:]*):)?(.*)");
	
	private LocalVariableTableParameterNameDiscoverer parmDiscover = new LocalVariableTableParameterNameDiscoverer();

	private final String MVC_SUFFIX = "MVCBinder";
	
	private final String HARNESS_VAR = "harness";
	private final String CONTROLLER_VAR = "controller";
	
	private final Class<?>[] proxyable = new Class<?>[] {
			ExceptionHandler.class, 
			InitBinder.class 	
	};

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public Class<?> bindEndpoints(
			String beanName, 
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
		addControllerReference(mvcProxyClass, clazz, beanName, cp);
		
		// Add the member variable referencing the StatefulController
		//
		addFSMHarnessReference(mvcProxyClass, refFactory.getFSMHarnessId(), cp);
		
		// Copy methods that have a Transition annotation from the Stateful Controller to the Binder
		//
		addRequestMethods(mvcProxyClass, eventMapping, cp);
		
		// Copy Proxy methods that bypass the FSM
		//
		addProxyMethods(mvcProxyClass, clazz, cp);
		
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
	
	private void addControllerReference(
			CtClass mvcProxyClass,
			Class<?> clazz,
			String beanName, 
			ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(clazz.getName());
		CtField field = new CtField(type, CONTROLLER_VAR, mvcProxyClass);

		addResourceAnnotation(field, beanName);
		
		mvcProxyClass.addField(field);
	}

	private void addFSMHarnessReference(CtClass mvcProxyClass, String fsmHarnessId, ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(FSMHarness.class.getName());
		CtField field = new CtField(type, HARNESS_VAR, mvcProxyClass);

		addResourceAnnotation(field, fsmHarnessId);
		
		mvcProxyClass.addField(field);
	}

	private void addRequestMethods(CtClass mvcProxyClass, Map<String,Method> eventMapping, ClassPool cp) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// Build a method for each Event
		//
		for(String event : eventMapping.keySet()) {
			addRequestMethod(mvcProxyClass, event, eventMapping.get(event), cp);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addProxyMethods(CtClass mvcProxyClass, Class<?> ctrlClass, ClassPool cp) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		for(Class<?> annotation : this.proxyable) {
			List<Method> methods = getMethodsAnnotatedWith(ctrlClass, (Class<java.lang.annotation.Annotation>)annotation);
			for(Method method : methods) {
				addProxyMethod(mvcProxyClass, method, cp);
			}
		}
	}
	
	private void addProxyMethod(CtClass mvcProxyClass, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// Create Method
		//
		CtClass returnClass = cp.get(method.getReturnType().getName());
		CtMethod ctMethod = new CtMethod(returnClass, "$_" + method.getName(), null, mvcProxyClass);

		// Clone method Annotations
		//
		addMethodAnnotations(ctMethod, method);
		
		// Copy parameters one-for-one
		//
		copyParameters(ctMethod, method, cp);

		// Add the Method    
		//
		addProxyMethodBody(ctMethod, method);
		
		// Add the Method to the Proxy class
		//
		mvcProxyClass.addMethod(ctMethod);
	}
	
	private void addRequestMethod(CtClass mvcProxyClass, String event, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		Pair<String, String> methodEndpoint = this.parseMethod(event);
		String requestMethod = methodEndpoint.getLeft();
		String requestEvent = methodEndpoint.getRight();
		requestMethod = (requestMethod == null) ? RequestMethod.GET.toString() : requestMethod.toUpperCase();

		
		// References id?
		//
		boolean referencesId = (event.indexOf("{id}") > 0);

		// Clone Method from the StatefulController
		//
		CtMethod ctMethod = createRequestMethod(mvcProxyClass, requestMethod, requestEvent, method, cp);

		// Clone method Annotations
		//
		addMethodAnnotations(ctMethod, method);

		// Add a RequestMapping annotation
		//
		addRequestMapping(ctMethod, requestMethod, requestEvent);

		// Clone the parameters, along with the Annotations
		//
		addRequestParameters(referencesId, ctMethod, method, cp);

		// Add the Method Body
		//
		addRequestMethodBody(referencesId, ctMethod, event);
		
		// Add the Method to the Proxy class
		//
		mvcProxyClass.addMethod(ctMethod);
	}
	
	private CtMethod createRequestMethod(
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

		CtClass returnClass = (method == null) ? CtClass.voidType : cp.get(method.getReturnType().getName());
		CtMethod ctMethod = new CtMethod(returnClass, methodName, null, mvcProxyClass);
		return ctMethod;
	}
	
	private void addMethodAnnotations(CtMethod ctMethod, Method method) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (method != null) {
			MethodInfo methodInfo = ctMethod.getMethodInfo();
			ConstPool constPool = methodInfo.getConstPool();
			for(java.lang.annotation.Annotation anno : method.getAnnotations()) {
				AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

				// If it's a Transition skip
				//
				Annotation clone = null;
				if (anno instanceof Transitions || anno instanceof Transition) {
					// skip
				} else {
					clone = cloneAnnotation(constPool, anno);
					attr.addAnnotation(clone);
					methodInfo.addAttribute(attr);
				}
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
		
		ArrayMemberValue methodVals = new ArrayMemberValue(constPool);
		EnumMemberValue methodVal = new EnumMemberValue(constPool);
		methodVal.setType(RequestMethod.class.getName());
		methodVal.setValue(method);
		methodVals.setValue(new MemberValue[]{methodVal});
		
		requestMapping.addMemberValue("method", methodVals);
		attr.addAnnotation(requestMapping);
		methodInfo.addAttribute(attr);
	}
	
	private void addRequestMethodBody(boolean referencesId, CtMethod ctMethod, String event) throws CannotCompileException, NotFoundException {
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
	
	private void addProxyMethodBody(CtMethod ctMethod, Method method) throws CannotCompileException, NotFoundException {
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
	
	private void addRequestParameters(boolean referencesId, CtMethod ctMethod, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {
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
	
	private void copyParameters(CtMethod ctMethod, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {
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
	
	private Annotation[] addHttpRequestParameter(CtMethod ctMethod, ClassPool cp) throws NotFoundException, CannotCompileException {
		// Map the HttpServletRequest class
		//
		CtClass ctParm = cp.get(HttpServletRequest.class.getName());
		
		// Add the parameter to the method
		//
		ctMethod.addParameter(ctParm);
		
		return new Annotation[]{};
		
	}
	
	private Annotation[] addIdParameter(CtMethod ctMethod, ClassPool cp) throws NotFoundException, CannotCompileException {
		// Clone the parameter Class
		//
		CtClass ctParm = cp.get(Long.class.getName());
		
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
	private Annotation[] createParameterAnnotations(
			String parmName, 
			MethodInfo methodInfo, 
			java.lang.annotation.Annotation[] annotations, 
			ConstPool parameterConstPool) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		List<Annotation> ctParmAnnotations = new LinkedList<Annotation>();

		for(java.lang.annotation.Annotation annotation : annotations) {
			Annotation clone = cloneAnnotation(parameterConstPool, annotation);
			
			// Special case: since Javaassist doesn't allow me to set the name of the parameter,
			// I need to ensure that RequestParam's value is set to the parm name if there isn't already
			// a value set
			//
			if (RequestParam.class.isAssignableFrom(annotation.annotationType())) {
				if ("".equals(((RequestParam)annotation).value()) && !StringUtils.isEmpty(parmName)) {
					MemberValue value = createMemberValue(parameterConstPool, parmName);
					clone.addMemberValue("value", value);
				}
			}
			
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
	private Annotation cloneAnnotation(ConstPool constPool, java.lang.annotation.Annotation annotation) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		logger.debug("cloneAnnotation : Create annotation = {}", annotation.annotationType().getName());
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
		
	private Pair<String, String> parseMethod(String event) {
		Matcher matcher = this.methodPattern.matcher(event);
		if (!matcher.matches()) {
			throw new RuntimeException("Unable to parse event=" + event);
		}
		return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
	}

	private void addResourceAnnotation(CtField field, String beanName) {
		FieldInfo fi = field.getFieldInfo();
		
		AnnotationsAttribute attr = new AnnotationsAttribute(
				field.getFieldInfo().getConstPool(), 
				AnnotationsAttribute.visibleTag);
		Annotation annot = new Annotation(Resource.class.getName(), fi.getConstPool());
		
		StringMemberValue nameValue = new StringMemberValue(fi.getConstPool());
		nameValue.setValue(beanName);
		annot.addMemberValue("name", nameValue);
		
		attr.addAnnotation(annot);
		fi.addAttribute(attr);
	}
	
	private List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends java.lang.annotation.Annotation> annotation) {
	    final List<Method> methods = new ArrayList<Method>();
	    Class<?> clazz = type;
	    while (clazz != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
	        // iterate though the list of methods declared in the class represented by clazz variable, and add those annotated with the specified annotation
	        final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(clazz.getDeclaredMethods()));       
	        for (final Method method : allMethods) {
	            if (annotation == null || method.isAnnotationPresent(annotation)) {
	                methods.add(method);
	            }
	        }
	        // move to the upper class in the hierarchy in search for more methods
	        clazz = clazz.getSuperclass();
	    }
	    return methods;
	}
}
