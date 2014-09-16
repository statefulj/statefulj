package org.statefulj.framework.binders.camel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
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

import org.apache.camel.Consume;
import org.apache.camel.component.bean.BeanInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
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
			Field idField = getAnnotatedField(msg.getClass(), Id.class);
			if (idField != null) {
				try {
					idField.setAccessible(true);
					id = idField.get(msg);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			} else {
				try {
					idField = msg.getClass().getField("id");
					if (idField != null) {
						idField.setAccessible(true);
						id = idField.get(msg);
					}
				} catch (NoSuchFieldException e) {
					throw new RuntimeException(e);
				} catch (SecurityException e) {
					throw new RuntimeException(e);
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
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory)
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		logger.debug("bindEndpoints : Building Consumer for {}", controllerClass);
		
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
				"createMethod : Create method {} for {}", 
				methodName,
				camelProxyClass.getSimpleName());

		CtMethod ctMethod = new CtMethod(CtClass.voidType, methodName, null, camelProxyClass);
		return ctMethod;
	}
	
	private void addMethodAnnotations(CtMethod ctMethod, Method method) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (method != null) {
			MethodInfo methodInfo = ctMethod.getMethodInfo();
			ConstPool constPool = methodInfo.getConstPool();
			for(java.lang.annotation.Annotation anno : method.getAnnotations()) {
				AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

				// If it's a Transition skip
				//
				Annotation clone = null;
				if (anno instanceof Transitions || anno instanceof Transition) {
					// skip
				} else {
					clone = cloneAnnotation(constPool, anno);
					attr.addAnnotation(clone);
					methodInfo.addAttribute(attr);
				}
			}
		}
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
					"Object id = org.statefulj.framework.binder.camel.CamelBinder.lookupId($1); " +
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
	
	
	/**
	 * Clone an annotation and all of it's methods
	 * @param constPool
	 * @param annotation
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Annotation cloneAnnotation(ConstPool constPool, java.lang.annotation.Annotation annotation) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		logger.debug("cloneAnnotation : Create annotation = {}", annotation.annotationType().getName());
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
		
	private MemberValue createMemberValue(ConstPool constPool, Object val) {
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

	private void addResourceAnnotation(CtField field, String beanName) {
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
	
	private static 	Field getAnnotatedField(
			Class<?> clazz,
			Class<? extends java.lang.annotation.Annotation> annotationClass) {
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
