package org.statefulj.framework.core.mocks;

import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;

public class MockPersistenceSupportBeanFactory implements
		PersistenceSupportBeanFactory {

	@Override
	public Class<?> getKey() {
		// TODO Auto-generated method stub
		return CrudRepository.class;
	}

	@Override
	public String registerPersistenceSupport(Class<?> statefulClass,
			Class<?> statefulControllerClass, String startStateId,
			List<RuntimeBeanReference> stateBeans, String repoFactoryBeanId,
			BeanDefinitionRegistry reg) {
		return "mockPersistenceSupportId";
	}

	@Override
	public String registerHarness(Class<?> statefulClass,
			Class<?> statefulControllerClass, String fsmBeanId,
			String persistenceSupportId, BeanDefinitionRegistry reg) {
		return "mockHarnessId";
	}

}
