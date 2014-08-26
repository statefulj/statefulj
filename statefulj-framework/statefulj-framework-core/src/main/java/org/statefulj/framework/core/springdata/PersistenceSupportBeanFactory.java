package org.statefulj.framework.core.springdata;

import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public interface PersistenceSupportBeanFactory {

	/**
	 * Associate this PersistenceSupportBeanFactory with a Spring Data Repo Factory
	 * 
	 * @return Class of the Repo Factory
	 */
	Class<?> getKey(); 
	
	String registerPersistenceSupport(
			Class<?> statefulClass, 
			Class<?> statefulControllerClass, 
			String startStateId, 
			List<RuntimeBeanReference> stateBeans, 
			String repoFactoryBeanId,
			BeanDefinitionRegistry reg);

	String registerHarness(
			Class<?> statefulClass, 
			Class<?> statefulControllerClass, 
			String fsmBeanId,
			String persistenceSupportId,
			BeanDefinitionRegistry reg);

}
