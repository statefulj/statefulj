package org.statefulj.common.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
