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
