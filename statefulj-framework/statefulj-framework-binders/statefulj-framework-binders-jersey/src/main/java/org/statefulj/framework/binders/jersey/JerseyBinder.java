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

import static org.statefulj.framework.binders.common.utils.JavassistUtils.cloneAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import org.statefulj.framework.binders.common.AbstractRestfulBinder;

public class JerseyBinder extends AbstractRestfulBinder {
	
	public final static String KEY = "jersey";

	private final String JERSEY_SUFFIX = "JerseyBinder";
	
	@Override
	public String getKey() {
		return KEY;
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

		AnnotationsAttribute pathAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation pathMapping = new Annotation(Path.class.getName(), constPool);
		
		StringMemberValue valueVal = new StringMemberValue(constPool);
		valueVal.setValue(request);
		
		pathMapping.addMemberValue("value", valueVal);
		
		pathAttr.addAnnotation(pathMapping);
		methodInfo.addAttribute(pathAttr);

		// Add Verb Annotation (GET|POST|PUT|DELETE)
		//
		String verbClassName = "javax.ws.rs." + method;
		AnnotationsAttribute verbAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation verb = new Annotation(verbClassName, constPool);

		verbAttr.addAnnotation(verb);
		methodInfo.addAttribute(verbAttr);

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
