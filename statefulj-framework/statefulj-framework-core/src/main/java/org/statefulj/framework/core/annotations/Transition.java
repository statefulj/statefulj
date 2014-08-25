package org.statefulj.framework.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Transition {
	
	public final String ANY_STATE = "*";
	
	String from() default ANY_STATE;
	
	String event() default "";

	String to() default ANY_STATE;
	
}
