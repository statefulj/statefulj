package org.statefulj.common.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by andrewhall on 7/24/16.
 */
public class FieldAccessor<T, V> {

    protected Class<T> clazz;
    protected Field field;
    protected Method getMethod;
    protected Method setMethod;

    public FieldAccessor(Class<T> clazz, Field field) {
        try {
            this.clazz = clazz;
            this.field = field;
            init(field, clazz);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    public V getValue(T stateful) {
        try {
            if (this.getMethod != null) {
                return (V)this.getMethod.invoke(stateful);
            } else {
                return (V)this.field.get(stateful);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void setValue(T stateful, String state) {
        try {
            if (this.setMethod != null) {
                this.setMethod.invoke(stateful, state);
            } else {
                this.field.set(stateful, state);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public Field getField() {
        return field;
    }

    public Method getGetMethod() {
        return getMethod;
    }

    public Method getSetMethod() {
        return setMethod;
    }

    protected void init(Field field, Class<T> clazz) throws IntrospectionException {
        PropertyDescriptor statePropertyDescriptor = null;
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        for(PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (propertyDescriptor.getName().equals(field.getName())) {
                statePropertyDescriptor = propertyDescriptor;
                break;
            }
        }
        this.getMethod = (statePropertyDescriptor != null)
                ? statePropertyDescriptor.getReadMethod()
                : null;
        if (this.getMethod != null) {
            this.getMethod.setAccessible(true);
        }

        this.setMethod = (statePropertyDescriptor != null)
                ? statePropertyDescriptor.getWriteMethod()
                : null;
        if (this.setMethod != null) {
            this.setMethod.setAccessible(true);
        }
        if (this.getMethod == null || this.setMethod == null) {
            field.setAccessible(true);
        }
    }

}
