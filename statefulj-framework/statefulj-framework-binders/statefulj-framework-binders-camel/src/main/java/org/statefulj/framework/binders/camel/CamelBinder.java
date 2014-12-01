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
package org.statefulj.framework.binders.camel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.persistence.Id;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import org.apache.camel.Consume;
import org.apache.camel.component.bean.BeanInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.*;

import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.FSMHarness;
import org.statefulj.framework.core.model.ReferenceFactory;

public class CamelBinder implements EndpointBinder {
	
	public final static String KEY = "camel";

	private Logger logger = LoggerFactory.getLogger(CamelBinder.class);

	private final String CONSUMER_SUFFIX = "CamelBinder";
	
	private final String HARNESS_VAR = "harness";

	@Override
	public String getKey() {
		return KEY;
	}

	public static Object lookupId(Object msg) {
		Object id = null;
		if (msg instanceof String || Number.class.isAssignableFrom(msg.getClass())) {
			id = msg;
		} else {
			if (BeanInvocation.class.isAssignableFrom(msg.getClass())) {
				msg = ((BeanInvocation)msg).getArgs()[0];
			}
			Field idField = ReflectionUtils.getFirstAnnotatedField(msg.getClass(), Id.class);
			if (idField == null) {
				idField = ReflectionUtils.getFirstAnnotatedField(msg.getClass(), org.springframework.data.annotation.Id.class);
			}
			if (idField == null) {
				try {
					idField = msg.getClass().getField("id");
				} catch (Exception e) {
					// swallow
				}
			}
			if (idField != null) {
				try {
					idField.setAccessible(true);
					id = idField.get(msg);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return id;
	}
	
	@Override
	public Class<?> bindEndpoints(
			String beanName, 
			Class<?> controllerClass,
			Class<?> idType,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory)
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		logger.debug("Building Consumer for {}", controllerClass);
		
		// Set up the ClassPool
		//
		ClassPool cp = ClassPool.getDefault();
		cp.appendClassPath(new ClassClassPath(getClass()));

		// Create a new Consumer Class 
		//
		String camelProxyClassName = controllerClass.getName() + CONSUMER_SUFFIX;
		CtClass camelProxyClass = cp.makeClass(camelProxyClassName);
		
		// Add the member variable referencing the Harness
		//
		addFSMHarnessReference(camelProxyClass, refFactory.getFSMHarnessId(), cp);
		
		// Copy methods that have a Transition annotation from the Stateful Controller to the Binder
		//
		addConsumerMethods(camelProxyClass, eventMapping, cp);
		
		// Construct and return the Proxy Class
		//
		return camelProxyClass.toClass();
	}
	
	private void addFSMHarnessReference(CtClass camelProxyClass, String fsmHarnessId, ClassPool cp) throws NotFoundException, CannotCompileException {
		CtClass type = cp.get(FSMHarness.class.getName());
		CtField field = new CtField(type, HARNESS_VAR, camelProxyClass);

		addResourceAnnotation(field, fsmHarnessId);
		
		camelProxyClass.addField(field);
	}

	private void addConsumerMethods(CtClass camelProxyClass, Map<String,Method> eventMapping, ClassPool cp) throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {
		
		// Build a method for each Event
		//
		for(String event : eventMapping.keySet()) {
			addConsumerMethod(camelProxyClass, event, eventMapping.get(event), cp);
		}
	}
	
	private void addConsumerMethod(CtClass camelProxyClass, String event, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		// Clone Method from the StatefulController
		//
		CtMethod ctMethod = createConsumerMethod(camelProxyClass, event, method, cp);

		// Clone method Annotations
		//
		addMethodAnnotations(ctMethod, method);

		// Add a RequestMapping annotation
		//
		addConsumeAnnotation(ctMethod, event);

		// Clone the parameters, along with the Annotations
		//
		addMessageParameter(ctMethod, method, cp);

		// Add the Method Body
		//
		addMethodBody(ctMethod, event);
		
		// Add the Method to the Proxy class
		//
		camelProxyClass.addMethod(ctMethod);
	}
	
	private CtMethod createConsumerMethod(
			CtClass camelProxyClass, 
			String event, 
			Method method, 
			ClassPool cp) throws NotFoundException {
		String methodName = ("$_" + event.replaceAll("[/:\\.]", "_").replace("{", "").replace("}", "")).toLowerCase();

		logger.debug(
				"Create method {} for {}", 
				methodName,
				camelProxyClass.getSimpleName());

		CtMethod ctMethod = new CtMethod(CtClass.voidType, methodName, null, camelProxyClass);
		return ctMethod;
	}
	
	private void addConsumeAnnotation(CtMethod ctMethod, String uri) {
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		ConstPool constPool = methodInfo.getConstPool();

		Annotation consume = new Annotation(Consume.class.getName(), constPool);
		StringMemberValue valueVal = new StringMemberValue(constPool);
		valueVal.setValue(uri);
		consume.addMemberValue("uri", valueVal);

		AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		attr.addAnnotation(consume);
		methodInfo.addAttribute(attr);
	}
	
	private void addMethodBody(CtMethod ctMethod, String event) throws CannotCompileException, NotFoundException {
		String methodBody = 
				"{ " +
					"Object id = org.statefulj.framework.binders.camel.CamelBinder.lookupId($1); " +
					"$proceed(\"" + event + "\", id, new Object[]{$1, $1});" +
				"}";

		ctMethod.setBody(methodBody, "this." + HARNESS_VAR, "onEvent");
	}
	
	private void addMessageParameter(CtMethod ctMethod, Method method, ClassPool cp) throws NotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, CannotCompileException {

		// Only one parameter - a message object
		//
		Class<?> msgClass = (method != null && method.getParameterTypes().length == 3) ? method.getParameterTypes()[2] : Object.class;
		CtClass ctParm = cp.get(msgClass.getName());
		
		// Add the parameter to the method
		//
		ctMethod.addParameter(ctParm);
	}
	
}
