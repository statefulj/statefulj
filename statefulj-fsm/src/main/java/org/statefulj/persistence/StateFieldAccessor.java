package org.statefulj.persistence;

import org.statefulj.common.utils.FieldAccessor;
import org.statefulj.persistence.annotations.State;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Created by andrewhall on 7/24/16.
 */
public class StateFieldAccessor<T> extends FieldAccessor<T, Serializable> {

    public StateFieldAccessor(Class<T> clazz, Field field) {
        super(clazz, field);
    }

    @Override
    protected void init(Field field, Class<T> clazz) throws IntrospectionException {
        State state = field.getDeclaredAnnotation(State.class);
        State.AccessorType accessorType = (state != null) ? state.accessorType() : State.AccessorType.AUTO;
        String getMethodName = (state != null) ? state.getMethodName() : "";
        String setMethodName = (state != null) ? state.setMethodName() : "";
        if (accessorType != State.AccessorType.PROPERTY) {
            PropertyDescriptor statePropertyDescriptor = null;
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            for(PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                if (propertyDescriptor.getName().equals(field.getName())) {
                    statePropertyDescriptor = propertyDescriptor;
                    break;
                }
            }

            try {
                if (getMethodName.equals("") && statePropertyDescriptor == null && accessorType == State.AccessorType.METHOD) {
                    throw new RuntimeException("No defined getter for " + this.field.getName());
                }
                this.getMethod = (getMethodName.equals(""))
                        ? (statePropertyDescriptor != null) ? statePropertyDescriptor.getReadMethod() : null
                        : clazz.getMethod(getMethodName);
                if (this.getMethod != null) {
                    this.getMethod.setAccessible(true);
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to locate method = " + getMethodName);
            }

            try {
                if (setMethodName.equals("") && statePropertyDescriptor == null && accessorType == State.AccessorType.METHOD) {
                    throw new RuntimeException("No defined setter for " + this.field.getName());
                }
                this.setMethod = (setMethodName.equals(""))
                        ? (statePropertyDescriptor != null) ? statePropertyDescriptor.getWriteMethod() : null
                        : clazz.getMethod(setMethodName, String.class);
                if (this.setMethod != null) {
                    this.setMethod.setAccessible(true);
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to locate method = " + setMethodName);
            }

            if (accessorType == State.AccessorType.METHOD && (this.getMethod == null || this.setMethod == null)) {
                throw new RuntimeException("Unable to locate both a setter and getter for the state field for Class=" + this.clazz.getCanonicalName());
            }

            if (this.getMethod == null || this.setMethod == null) {
                this.field.setAccessible(true);
            }

        } else {
            this.field.setAccessible(true);
        }
    }

}
