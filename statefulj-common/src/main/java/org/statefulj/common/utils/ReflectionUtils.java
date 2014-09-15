package org.statefulj.common.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ReflectionUtils {
	
	public static Field getAnnotatedField(
			Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		Field match = null;
		if (clazz != null) {
			match = getAnnotatedField(clazz.getSuperclass(), annotationClass);
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
}
