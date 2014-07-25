package org.statefulj.webapp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;
import org.statefulj.fsm.FSMConstants;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Transition {
	
	String from() default FSMConstants.ANY_STATE;
	
	String event() default "";

	String to() default FSMConstants.ANY_STATE;
}
