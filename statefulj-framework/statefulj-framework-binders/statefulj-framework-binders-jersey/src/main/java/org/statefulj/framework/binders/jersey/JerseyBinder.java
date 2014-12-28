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
package org.statefulj.framework.binders.jersey;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.addClassAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.binders.common.AbstractRestfulBinder;
import org.statefulj.framework.core.model.ReferenceFactory;

public class JerseyBinder extends AbstractRestfulBinder {
	
	private Logger logger = LoggerFactory.getLogger(JerseyBinder.class);
	
	public final static String KEY = "jersey";

	private final String JERSEY_SUFFIX = "JerseyBinder";
	
	@Override
	public String getKey() {
		return KEY;
	}

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
		
		Class<?> binding = super.bindEndpoints(
				beanName, 
				statefulControllerClass, 
				idType, 
				isDomainEntity,
				eventMapping, 
				refFactory);
		
		// Add to registry so that the class can be passed to the Jersey ResourceConfig
		//
		BindingsRegistry.addBinding(binding);
		
		return binding;
	}

	@Override
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
		
		logger.debug("Building proxy for {}", statefulControllerClass);
		
		CtClass proxyClass = super.buildProxy(
				cp,
				beanName, 
				proxyClassName, 
				statefulControllerClass, 
				idType,
				isDomainEntity,
				eventMapping, 
				refFactory);
		
		// Add Path Annotation
		//
		addClassAnnotation(proxyClass, Path.class, "value", "");
		
		return proxyClass;
	}
	
	@Override
	protected void addEndpointMapping(
			CtMethod ctMethod, 
			String method,
			String request) {
		
		// Add Path Annotation
		//
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();

		AnnotationsAttribute annoAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation pathMapping = new Annotation(Path.class.getName(), constPool);
		
		StringMemberValue valueVal = new StringMemberValue(constPool);
		valueVal.setValue(request);
		
		pathMapping.addMemberValue("value", valueVal);
		
		annoAttr.addAnnotation(pathMapping);

		// Add Verb Annotation (GET|POST|PUT|DELETE)
		//
		String verbClassName = "javax.ws.rs." + method;
		Annotation verb = new Annotation(verbClassName, constPool);
		annoAttr.addAnnotation(verb);
		
		methodInfo.addAttribute(annoAttr);
	}

	@Override
	protected Annotation[] addHttpRequestParameter(CtMethod ctMethod, ClassPool cp) throws NotFoundException, CannotCompileException {

		super.addHttpRequestParameter(ctMethod, cp);
		
		return new Annotation[] {
				new Annotation(
					ctMethod.getMethodInfo().getConstPool(), 
					cp.getCtClass(Context.class.getName())
				) };
	}

	@Override
	protected String getSuffix() {
		return this.JERSEY_SUFFIX;
	}

	@Override
	protected Class<?> getPathAnnotationClass() {
		return PathParam.class;
	}

}
