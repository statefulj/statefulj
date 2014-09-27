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
package org.statefulj.common.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

public class ReflectionUtils {
	
	public static Field getFirstAnnotatedField(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) { 
		Field match = null;
		if (clazz != null) {
			match = getFirstAnnotatedField(clazz.getSuperclass(), annotationClass);
			if (match == null) {
				for(Field field : clazz.getDeclaredFields()) {
					if (field.isAnnotationPresent(annotationClass)) {
						match = field;
						break;
					}
				}
				
			}
		}
		
		return match;
	}
	
	public static List<Field> getAllAnnotatedFields(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		List<Field> fields = new LinkedList<Field>();
		if (clazz != null) {
			fields.addAll(getAllAnnotatedFields(clazz.getSuperclass(), annotationClass));
			for(Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(annotationClass)) {
					fields.add(field);
				}
			}
		}
		
		return fields;
	}
}
