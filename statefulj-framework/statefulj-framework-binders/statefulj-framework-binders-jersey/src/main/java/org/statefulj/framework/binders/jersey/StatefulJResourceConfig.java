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
package org.statefulj.framework.binders.jersey;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

public class StatefulJResourceConfig extends ResourceConfig {

    public StatefulJResourceConfig() {
    	super();
    	registerAllBindings();
    }

    /**
     * Create a new resource configuration initialized with a given set of
     * resource/provider classes.
     *
     * @param classes application-specific resource and/or provider classes.
     */
    public StatefulJResourceConfig(final Set<Class<?>> classes) {
    	super(classes);
    	registerAllBindings();
    }

    /**
     * Create a new resource configuration initialized with a given set of
     * resource/provider classes.
     *
     * @param classes application-specific resource and/or provider classes.
     */
    public StatefulJResourceConfig(final Class<?>... classes) {
    	super(classes);
    	registerAllBindings();
    }

    /**
     * Create a defensive resource configuration copy initialized with a given {@code StatefulJResourceConfig}.
     *
     * @param original resource configuration to create a defensive copy from.
     */
    public StatefulJResourceConfig(final StatefulJResourceConfig original) {
    	super(original);
    	registerAllBindings();
    }
    
    public StatefulJResourceConfig registerAllBindings() {
    	for(Class<?> binding : BindingsRegistry.getAllBindings()) {
    		register(binding);
    	}
    	return this;
    }

    public StatefulJResourceConfig registerBindingsByPackage(String pkg) {
    	for(Class<?> binding : BindingsRegistry.getBindingsByPkg(pkg)) {
    		register(binding);
    	}
    	return this;
    }
}
