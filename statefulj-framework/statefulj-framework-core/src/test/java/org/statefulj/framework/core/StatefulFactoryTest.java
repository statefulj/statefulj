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
package org.statefulj.framework.core;

import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.controllers.NoRetryController;
import org.statefulj.framework.core.controllers.UserController;
import org.statefulj.framework.core.dao.UserRepository;
import org.statefulj.framework.core.mocks.MockBeanDefinitionRegistryImpl;
import org.statefulj.framework.core.mocks.MockProxy;
import org.statefulj.framework.core.mocks.MockRepositoryFactoryBeanSupport;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;

public class StatefulFactoryTest {
	
	
	@Test
	public void testFSMConstruction() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition userRepo = BeanDefinitionBuilder
				.genericBeanDefinition(MockRepositoryFactoryBeanSupport.class)
				.getBeanDefinition();
		userRepo.getPropertyValues().add("repositoryInterface", UserRepository.class.getName());

		registry.registerBeanDefinition("userRepo", userRepo);
	
		BeanDefinition userController = BeanDefinitionBuilder
				.genericBeanDefinition(UserController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("userController", userController);
	
		ReferenceFactory refFactory = new ReferenceFactoryImpl("userController");
		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);
		
		BeanDefinition userControllerMVCProxy = registry.getBeanDefinition(refFactory.getBinderId("mock"));
		
		assertNotNull(userControllerMVCProxy);
		
		Class<?> proxyClass = Class.forName(userControllerMVCProxy.getBeanClassName());
		
		assertNotNull(proxyClass);
		
		assertEquals(MockProxy.class, proxyClass);
		
		// Verify that FIVE_STATE is blocking
		//
		BeanDefinition stateFive = registry.getBeanDefinition(refFactory.getStateId(UserController.FIVE_STATE));
		
		assertEquals(true, stateFive.getConstructorArgumentValues().getArgumentValue(2, Boolean.class).getValue());
		
		// Verify that the FSM has a RetryObserver
		//
		BeanDefinition retryObserver = registry.getBeanDefinition(refFactory.getRetryObserverId());
		assertNotNull(retryObserver);
		
		BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
		assertNotNull(fsm);
		assertEquals(20, fsm.getConstructorArgumentValues().getArgumentValue(2, Integer.class).getValue());
		assertEquals(250, fsm.getConstructorArgumentValues().getArgumentValue(3, Integer.class).getValue());
		assertEquals(
				new RuntimeBeanReference(refFactory.getRetryObserverId()), 
				fsm.
					getConstructorArgumentValues().
					getArgumentValue(4, RuntimeBeanReference.class).
					getValue());
		
	}
 
	@Test
	public void testFSMConstructionWithoutRetry() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		
		BeanDefinitionRegistry registry = new MockBeanDefinitionRegistryImpl();
		
		BeanDefinition userRepo = BeanDefinitionBuilder
				.genericBeanDefinition(MockRepositoryFactoryBeanSupport.class)
				.getBeanDefinition();
		userRepo.getPropertyValues().add("repositoryInterface", UserRepository.class.getName());

		registry.registerBeanDefinition("userRepo", userRepo);
	
		BeanDefinition noRetryController = BeanDefinitionBuilder
				.genericBeanDefinition(NoRetryController.class)
				.getBeanDefinition();

		registry.registerBeanDefinition("noRetryController", noRetryController);
	
		ReferenceFactory refFactory = new ReferenceFactoryImpl("noRetryController");
		StatefulFactory factory = new StatefulFactory();
		
		factory.postProcessBeanDefinitionRegistry(registry);

		// Verify that the FSM has a RetryObserver
		//
		BeanDefinition retryObserver = registry.getBeanDefinition(refFactory.getRetryObserverId());
		assertNull(retryObserver);
		
		BeanDefinition fsm = registry.getBeanDefinition(refFactory.getFSMId());
		assertNotNull(fsm);
		assertEquals(1, fsm.getConstructorArgumentValues().getArgumentValue(2, Integer.class).getValue());
		assertEquals(1, fsm.getConstructorArgumentValues().getArgumentValue(3, Integer.class).getValue());
		assertNull(fsm.getConstructorArgumentValues().getArgumentValue(4, RuntimeBeanReference.class));
	}
 
}
