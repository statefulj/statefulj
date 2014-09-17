package org.statefulj.framework.core;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.StatefulFactory.FSMWiring;
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
		
		Map<String, Set<FSMWiring>> fsmWiringsMapping = factory.getFsmWirings();
		
		assertNotNull(fsmWiringsMapping);
		
		Set<FSMWiring> fsmWirings = fsmWiringsMapping.get("userController");
		
		assertNotNull(fsmWirings);
		
		FSMWiring fsmWiring = fsmWirings.iterator().next();

		assertNotNull(fsmWiring);
		assertEquals("fsm", fsmWiring.getField().getName());
		assertEquals("userController.statefulFSM", fsmWiring.getFsmId());
	}
 
}
