package org.statefulj.framework.persistence.jpa;

import java.beans.Introspector;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.statefulj.framework.core.model.impl.CrudRepositoryFinderImpl;
import org.statefulj.framework.core.model.impl.FactoryImpl;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;
import org.statefulj.persistence.jpa.JPAPerister;

public class JPAPersistenceSupportBeanFactory implements PersistenceSupportBeanFactory {

	public static String FSM_HARNESS_SUFFIX = "FSMHarness";

	@Override
	public Class<?> getKey() {
		return JpaRepositoryFactoryBean.class;
	}

	@Override
	public String registerFactory(
			Class<?> statefulClass,
			Class<?> statefulControllerClass,
			BeanDefinitionRegistry reg) {
		String factoryId = Introspector.decapitalize(statefulControllerClass.getSimpleName() + ".factory");
		BeanDefinition factoryBean = BeanDefinitionBuilder
				.genericBeanDefinition(FactoryImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = factoryBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, statefulClass);
		reg.registerBeanDefinition(factoryId, factoryBean);
		return factoryId;
	}

	@Override
	public String registerFinder(
			Class<?> statefulControllerClass,
			String repoFactoryBeanId,
			BeanDefinitionRegistry reg) {
		String finderId = Introspector.decapitalize(statefulControllerClass.getSimpleName() + ".finder");
		BeanDefinition finderBean = BeanDefinitionBuilder
				.genericBeanDefinition(CrudRepositoryFinderImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = finderBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(repoFactoryBeanId));
		reg.registerBeanDefinition(finderId, finderBean);
		return finderId;
	}

	@Override
	public String registerPersister(
			Class<?> statefulClass,
			Class<?> statefulControllerClass,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans,
			BeanDefinitionRegistry reg) {
		
		String persisterId = Introspector.decapitalize(statefulControllerClass.getSimpleName() + ".persister");
		BeanDefinition persisterBean = BeanDefinitionBuilder
				.genericBeanDefinition(JPAPerister.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, stateBeans);
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(startStateId));
		args.addIndexedArgumentValue(2, statefulClass);
		reg.registerBeanDefinition(persisterId, persisterBean);
		
		return persisterId;
	}

	
	@Override
	public String registerHarness(
			Class<?> statefulClass,
			Class<?> statefulControllerClass, 
			String fsmBeanId,
			String factoryId, 
			String finderId, 
			BeanDefinitionRegistry reg) {

		// Build the FSMHarness
		//
		String fsmHarnessId = Introspector.decapitalize(statefulControllerClass.getSimpleName() + FSM_HARNESS_SUFFIX);
		BeanDefinition fsmHarness = BeanDefinitionBuilder
				.genericBeanDefinition(JPAFSMHarnessImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = fsmHarness.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
		args.addIndexedArgumentValue(1, statefulClass);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
		args.addIndexedArgumentValue(3, new RuntimeBeanReference(finderId));
		reg.registerBeanDefinition(fsmHarnessId, fsmHarness);		
		return fsmHarnessId;
	}

}
