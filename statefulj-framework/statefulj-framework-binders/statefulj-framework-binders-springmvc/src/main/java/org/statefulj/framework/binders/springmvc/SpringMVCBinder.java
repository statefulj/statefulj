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
package org.statefulj.framework.binders.springmvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.*;

import org.statefulj.framework.binders.common.AbstractRestfulBinder;
import org.statefulj.framework.core.model.ReferenceFactory;

// TODO : Handle when an action doesn't have either the User or Event parameter
public class SpringMVCBinder extends AbstractRestfulBinder {
	
	public final static String KEY = "springmvc";

	private Logger logger = LoggerFactory.getLogger(SpringMVCBinder.class);
	
	private final String MVC_SUFFIX = "MVCBinder";
	
	private final Class<?>[] proxyable = new Class<?>[] {
			ExceptionHandler.class, 
			InitBinder.class 	
	};

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	protected CtClass buildProxy(
			ClassPool cp,
			String beanName, 
			String proxyClassName,
			Class<?> statefulControllerClass,
			Class<?> idType,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory) 
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		
		logger.debug("Building proxy for {}", statefulControllerClass);
		
		CtClass proxyClass = super.buildProxy(
				cp,
				beanName, 
				proxyClassName, 
				statefulControllerClass, 
				idType,
				eventMapping, 
				refFactory);
		
		// Copy Proxy methods that bypass the FSM
		//
		addProxyMethods(proxyClass, statefulControllerClass, cp);
		
		return proxyClass;
		
	}
	
	@Override
	protected Class<?> getComponentClass() {
		return Controller.class;
	}
	
	/**
	 * Clone all the parameter Annotations from the StatefulController to the Proxy
	 * 
	 * @param methodInfo
	 * @param parmIndex
	 * @param annotations
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@Override
	protected Annotation[] createParameterAnnotations(
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
			
			ctParmAnnotations.add(clone);
		}
		return ctParmAnnotations.toArray(new Annotation[]{});
	}
	
	@Override
	protected void addEndpointMapping(CtMethod ctMethod, String method, String request) {
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

	@Override
	protected String getSuffix() {
		return MVC_SUFFIX;
	}
	
	@Override
	protected Class<?> getPathAnnotationClass() {
		return PathVariable.class;
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

	private void addProxyMethodBody(CtMethod ctMethod, Method method) throws CannotCompileException, NotFoundException {
		String returnType = ctMethod.getReturnType().getName();
		
		String returnStmt = 
				(returnType.equals("void")) 
				? ""
				: "return (" + returnType + ")";
		
		String methodBody = "{ " 
				+ returnStmt
				+ "$proceed($$); }";

		ctMethod.setBody(methodBody, "this." + getControllerVar(), method.getName());
	}
	
}
