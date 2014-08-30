package org.statefulj.framework.core.mocks;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.statefulj.framework.core.model.impl.FactoryImpl;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;

public class MockPersistenceSupportBeanFactory implements
		PersistenceSupportBeanFactory {

	@Override
	public Class<?> getKey() {
		return MockRepositoryFactoryBeanSupport.class;
	}

	@Override
	public BeanDefinition buildFactoryBean(Class<?> statefulClass) {
		return mockDef();
	}

	@Override
	public BeanDefinition buildFinderBean(String repoBeanId) {
		return mockDef();
	}

	@Override
	public BeanDefinition buildPersisterBean(Class<?> statefulClass,
			String startStateId, List<RuntimeBeanReference> stateBeans) {
		return mockDef();
	}

	@Override
	public BeanDefinition buildHarnessBean(Class<?> statefulClass,
			String fsmBeanId, String factoryId, String finderId) {
		return mockDef();
	}
	
	private BeanDefinition mockDef() {
		return BeanDefinitionBuilder
				.genericBeanDefinition(FactoryImpl.class)
				.getBeanDefinition();
	}
 
}
