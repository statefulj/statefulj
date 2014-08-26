package org.statefulj.framework.persistence.jpa;

import java.beans.Introspector;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.statefulj.framework.core.model.impl.PersistenceSupportImpl;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;
import org.statefulj.persistence.jpa.JPAPerister;

public class JPAPersistenceSupportBeanFactory implements PersistenceSupportBeanFactory {

	public static String FSM_HARNESS_SUFFIX = "FSMHarness";

	@Override
	public Class<?> getKey() {
		return JpaRepositoryFactoryBean.class;
	}

	@Override
	public String registerPersistenceSupport(
			Class<?> statefulClass,
			Class<?> statefulControllerClass,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans,
			String repoFactoryBeanId, 
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
		
		String persistenceSupportId = Introspector.decapitalize(statefulControllerClass.getSimpleName() + ".presistenceSupport");
		BeanDefinition persistenceSupportBean = BeanDefinitionBuilder
				.genericBeanDefinition(PersistenceSupportImpl.class)
				.getBeanDefinition();
		args = persistenceSupportBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(repoFactoryBeanId));
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(persisterId));
		reg.registerBeanDefinition(persistenceSupportId, persistenceSupportBean);

		return persistenceSupportId;
	}

	@Override
	public String registerHarness(
			Class<?> statefulClass, 
			Class<?> statefulControllerClass,
			String fsmBeanId,
			String persistenceSupportId,
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
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(persistenceSupportId));
		reg.registerBeanDefinition(fsmHarnessId, fsmHarness);		
		return fsmHarnessId;
	}
}
