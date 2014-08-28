package org.statefulj.framework.core.springdata;

import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.statefulj.framework.core.model.Factory;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.fsm.FSM;

public interface PersistenceSupportBeanFactory {

	/**
	 * Associate this PersistenceSupportBeanFactory with a Spring Data Repo Factory
	 * 
	 * @return Class of the Repo Factory
	 */
	Class<?> getKey(); 
	
	String registerFactory(
			Class<?> statefulClass,
			Class<?> statefulControllerClass,
			BeanDefinitionRegistry reg);
	
	String registerFinder(
			Class<?> statefulControllerClass,
			String repoFactoryBeanId,
			BeanDefinitionRegistry reg);
	
	String registerPersister(
			Class<?> statefulClass,
			Class<?> statefulControllerClass,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans,
			BeanDefinitionRegistry reg);

	String registerHarness(
			Class<?> statefulClass, 
			Class<?> statefulControllerClass, 
			String fsmBeanId,
			String factoryId,
			String finderId,
			BeanDefinitionRegistry reg);

}
