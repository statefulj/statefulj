package org.statefulj.framework.core;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.controllers.UserController;
import org.statefulj.framework.core.mocks.BeanDefinitionRegistryImpl;

public class StatefulFactoryTest {
	
	@Test
	public void testFSMConstruction() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		
		BeanDefinitionRegistry registry = new BeanDefinitionRegistryImpl();
		
		BeanDefinition userController = BeanDefinitionBuilder
				.genericBeanDefinition(UserController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("userController", userController);
		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);
		
		BeanDefinition userControllerMVCProxy = registry.getBeanDefinition("userControllerMVCProxy");
		
		assertNotNull(userControllerMVCProxy);
		
		Class<?> proxyClass = Class.forName(userControllerMVCProxy.getBeanClassName());
		
		assertNotNull(proxyClass);
		
		Method method = proxyClass.getDeclaredMethod("$_get_first");
		
		assertNotNull(method);
		
		method = proxyClass.getDeclaredMethod("$_post_id_second", Long.class);
		
		assertNotNull(method);
		
	}
 
}
