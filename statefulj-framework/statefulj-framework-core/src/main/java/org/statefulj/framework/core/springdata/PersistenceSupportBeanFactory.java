package org.statefulj.framework.core.springdata;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
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
	
	BeanDefinition buildFactoryBean(Class<?> statefulClass);
	
	BeanDefinition buildFinderBean(String repoBeanId);
	
	BeanDefinition buildPersisterBean(
			Class<?> statefulClass,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans);

	BeanDefinition buildFSMHarnessBean(
			Class<?> statefulClass, 
			String fsmBeanId,
			String factoryId,
			String finderId);

}
