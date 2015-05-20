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
package org.statefulj.framework.binders.common.utils;

import static org.statefulj.framework.binders.common.utils.JavassistUtils.cloneAnnotation;
import static org.statefulj.framework.binders.common.utils.JavassistUtils.getAnnotationsAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
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

public class JavassistUtils {
	
	public static void addClassAnnotation(CtClass clazz, Class<?> annotationClass, Object... values) {
		ClassFile ccFile = clazz.getClassFile();
		ConstPool constPool = ccFile.getConstPool();
		AnnotationsAttribute attr = getAnnotationsAttribute(ccFile);
		Annotation annot = new Annotation(annotationClass.getName(), constPool);
		
		for(int i = 0; i < values.length; i = i + 2) {
			String valueName = (String)values[i];
			Object value = values[i+1];
			if (valueName != null && value != null) {
				MemberValue memberValue = createMemberValue(constPool, value);
				annot.addMemberValue(valueName, memberValue);
			}
		}
		
		attr.addAnnotation(annot);
	}
	
	public static AnnotationsAttribute getAnnotationsAttribute(ClassFile ccFile) {
		AnnotationsAttribute attr = (AnnotationsAttribute) ccFile.getAttribute(AnnotationsAttribute.visibleTag);
		if (attr == null) {
			attr = new AnnotationsAttribute(ccFile.getConstPool(), AnnotationsAttribute.visibleTag);
			ccFile.addAttribute(attr);
		}
		return attr;
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
	public static Annotation cloneAnnotation(ConstPool constPool, java.lang.annotation.Annotation annotation) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
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
		
	public static void addResourceAnnotation(CtField field, String beanName) {
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
	
	public static void addMethodAnnotations(CtMethod ctMethod, Method method) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (method != null) {
			MethodInfo methodInfo = ctMethod.getMethodInfo();
			ConstPool constPool = methodInfo.getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
			methodInfo.addAttribute(attr);
			for(java.lang.annotation.Annotation anno : method.getAnnotations()) {

				// If it's a Transition skip
				// TODO : Make this a parameterized set of Filters instead of hardcoding
				//
				Annotation clone = null;
				if (anno instanceof Transitions || anno instanceof Transition) {
					// skip
				} else {
					clone = cloneAnnotation(constPool, anno);
					attr.addAnnotation(clone);
				}
			}
		}
	}
	
	public static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends java.lang.annotation.Annotation> annotation) {
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

	
	public static MemberValue createMemberValue(ConstPool constPool, Object val) {
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

	public static void copyTypeAnnotations(Class<?> fromClass, CtClass toClass) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		for(java.lang.annotation.Annotation annotation :  fromClass.getAnnotations()) {
			if (!StatefulController.class.isAssignableFrom(annotation.annotationType())) {
				ClassFile ccFile = toClass.getClassFile();
				AnnotationsAttribute attr = getAnnotationsAttribute(ccFile);
				Annotation clone = cloneAnnotation(ccFile.getConstPool(), annotation);
				attr.addAnnotation(clone);
			}
		}
	}
}
