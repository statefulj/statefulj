package org.statefulj.common.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class that encapsulates access to a field.  If the field has a getter or setter
 * method, per JavaBean spec, the methods will be used; otherwise, the accessor will
 * directly read or update the field via reflection
 *
 * Created by andrewhall on 7/24/16.
 */
public class FieldAccessor<T, V> {

    protected Class<T> clazz;
    protected Field field;
    protected Method getMethod;
    protected Method setMethod;

    /**
     * Constructor for a Field Accessor.  Specify the Class and Fields
     *
     * @param clazz
     * @param field
     */
    public FieldAccessor(Class<T> clazz, Field field) {
        try {
            this.clazz = clazz;
            this.field = field;
            init(field, clazz);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the value of the object
     *
     * @param object
     * @return
     */
    public V getValue(T object) {
        try {
            if (this.getMethod != null) {
                return (V)this.getMethod.invoke(object);
            } else {
                return (V)this.field.get(object);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set's the value of the object
     *
     * @param object
     * @param value
     */
    public void setValue(T object, V value) {
        try {
            if (this.setMethod != null) {
                this.setMethod.invoke(object, value);
            } else {
                this.field.set(object, value);
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
