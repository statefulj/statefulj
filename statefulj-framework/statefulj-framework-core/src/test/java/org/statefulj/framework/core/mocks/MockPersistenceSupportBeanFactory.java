package org.statefulj.framework.core.mocks;

import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;

public class MockPersistenceSupportBeanFactory implements
		PersistenceSupportBeanFactory {

	@Override
	public Class<?> getKey() {
		return MockRepositoryFactoryBeanSupport.class;
	}

	@Override
	public String registerFactory(Class<?> statefulClass,
			Class<?> statefulControllerClass, BeanDefinitionRegistry reg) {
		return "mockFactoryId";
	}

	@Override
	public String registerFinder(Class<?> statefulControllerClass,
			String repoFactoryBeanId, BeanDefinitionRegistry reg) {
		return "mockFinderId";
	}

	@Override
	public String registerPersister(Class<?> statefulClass,
			Class<?> statefulControllerClass, String startStateId,
			List<RuntimeBeanReference> stateBeans, BeanDefinitionRegistry reg) {
		return "mockPersisterId";
	}

	@Override
	public String registerHarness(Class<?> statefulClass,
			Class<?> statefulControllerClass, String fsmBeanId,
			String factoryId, String finderId, BeanDefinitionRegistry reg) {
		return "mockHarnessId";
	}


}
