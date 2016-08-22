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
package org.statefulj.persistence.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the State field and the means to access the State field
 *
 * @author Andrew Hall
 *
 */
@Target({FIELD})
@Retention(RUNTIME)
public @interface State {

    enum AccessorType {
        AUTO,            // Will use method accessor, if not available, will update property directly
        PROPERTY,        // Will update property directly
        METHOD           // Will only use a method to access the property
    }

    /**
     * Specify how to access the State Field
     * @return
     */
    AccessorType accessorType() default AccessorType.AUTO;

    /**
     * If the name of the set method is not a standard JavaBean setter method, then specify
     * the name of the name of the setter method.  If accessorType is set to PROPERTY, this value is
     * ignored
     *
     * @return
     */
    String setMethodName() default "";

    /**
     * If the name of the get method is not a standard JavaBean getter method, then specify
     * the name of the name of the getter method.  If accessorType is set to PROPERTY, this value is
     * ignored
     *
     * @return
     */
    String getMethodName() default "";

}
