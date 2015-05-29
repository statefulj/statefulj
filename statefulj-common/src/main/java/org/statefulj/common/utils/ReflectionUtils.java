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
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReflectionUtils {
	
	private static Pattern fieldNamePattern = Pattern.compile("[g|s]et(.)(.*)");
	
	public static Field getFirstAnnotatedField(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) { 
		Field match = null;
		if (clazz != null) {
			for(Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(annotationClass)) {
					match = field;
					break;
				}
			}
			if (match == null) {
				match = getFirstAnnotatedField(clazz.getSuperclass(), annotationClass);
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
	
	public static Method getFirstAnnotatedMethod(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		Method match = null;
		if (clazz != null) {
			for(Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotationClass)) {
					match = method;
					break;
				}
			}
			if (match == null) {
				match = getFirstAnnotatedMethod(clazz.getSuperclass(), annotationClass);
			}
		}
		
		return match;
	}

	public static Class<?> getFirstAnnotatedClass(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		
		if (clazz == null) {
			return null;
		}
		
		Class<?> annotatedClass = (clazz.isAnnotationPresent(annotationClass)) ? clazz : null;
		
		if (annotatedClass == null ) {
			annotatedClass = getFirstAnnotatedClass(clazz.getSuperclass(), annotationClass);
		}
		
		return annotatedClass;
	}

	public static <T extends Annotation> T getFirstClassAnnotation(
			Class<?> clazz,
			Class<T> annotationClass) {
		
		if (clazz == null) {
			return null;
		}
		
		T annotation = clazz.getAnnotation(annotationClass);
		
		if (annotation == null ) {
			annotation = getFirstClassAnnotation(clazz.getSuperclass(), annotationClass);
		}
		
		return annotation;
	}

	public static boolean isAnnotationPresent(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		
		if (clazz == null) {
			return false;
		}
		
		boolean annotationPresent = clazz.isAnnotationPresent(annotationClass);
		
		if (!annotationPresent) {
			annotationPresent = isAnnotationPresent(clazz.getSuperclass(), annotationClass);
		}
		
		return annotationPresent;
	}

	public static boolean isGetter(Method method){
		  if(!method.getName().startsWith("get"))      return false;
		  if(method.getParameterTypes().length != 0)   return false;  
		  if(void.class.equals(method.getReturnType())) return false;
		  return true;
	}
	
	public static boolean isSetter(Method method){
	  if(!method.getName().startsWith("set")) return false;
	  if(method.getParameterTypes().length != 1) return false;
	  return true;
	}
	
	public static String toFieldName(Method getterOrSetter) {
		Matcher matcher = fieldNamePattern.matcher(getterOrSetter.getName());
		return (matcher.matches()) ? matcher.group(1).toLowerCase() + matcher.group(2) : null;
	}
		
	public static Field getReferencedField(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		Field field = getFirstAnnotatedField(clazz, annotationClass);
		if (field == null) {
			Method method = getFirstAnnotatedMethod(clazz, annotationClass);
			if (method != null && (isGetter(method) || isSetter(method))) {
				String fieldName = toFieldName(method);
				try {
					field = (fieldName != null) ? clazz.getDeclaredField(fieldName) : null;
				} catch (Exception e) {
					// Ignore
				}
				if (field == null) {
					try {
						field = (fieldName != null) ? clazz.getField(fieldName) : null;
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		}
		return field;
	}

    /**
     * Climb the class hierarchy starting with the clazz provided, looking for the field with fieldName
     *
     * @param clazz starting class to search at
     * @param fieldName name of the field we are looking for
     * @return Field which was found, or null if nothing was found
     */
	public static Field getField(final Class<?> clazz, final String fieldName) {
        //Define return type
        //
        Field
            field = null;

        //For each class in the hierarchy starting with the current class, try to find the declared field
        //
        for (Class<?> current = clazz; current != null && field == null; current = current.getSuperclass()) {
            try {
                //Attempt to get the field, if exception is thrown continue to the next class
                //
                field =
                    current
                        .getDeclaredField(
                            fieldName
                        );
            }
            catch (final NoSuchFieldException e) {
                //ignore and continue searching
                //
            }
        }

        //Return the field we found
        //
        return
            field;
    }
}
