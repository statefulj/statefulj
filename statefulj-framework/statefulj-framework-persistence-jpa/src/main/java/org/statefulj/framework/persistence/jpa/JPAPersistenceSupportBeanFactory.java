package org.statefulj.framework.persistence.jpa;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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
	public BeanDefinition buildFactoryBean(Class<?> statefulClass) {
		BeanDefinition factoryBean = BeanDefinitionBuilder
				.genericBeanDefinition(FactoryImpl.class)
				.getBeanDefinition();
		return factoryBean;
	}

	@Override
	public BeanDefinition buildFinderBean(String repoFactoryBeanId) {
		BeanDefinition finderBean = BeanDefinitionBuilder
				.genericBeanDefinition(CrudRepositoryFinderImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = finderBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(repoFactoryBeanId));
		return finderBean;
	}

	@Override
	public BeanDefinition buildPersisterBean(Class<?> statefulClass,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans) {
		BeanDefinition persisterBean = BeanDefinitionBuilder
				.genericBeanDefinition(JPAPerister.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, stateBeans);
		args.addIndexedArgumentValue(1, new RuntimeBeanReference(startStateId));
		args.addIndexedArgumentValue(2, statefulClass);
		return persisterBean;
	}

	
	@Override
	public BeanDefinition buildStatefulFSM(
			Class<?> statefulClass,
			String fsmBeanId,
			String factoryId, 
			String finderId) {

		BeanDefinition statefulFSM = BeanDefinitionBuilder
				.genericBeanDefinition(JPAStatefulFSMImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = statefulFSM.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
		args.addIndexedArgumentValue(1, statefulClass);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
		args.addIndexedArgumentValue(3, new RuntimeBeanReference(finderId));
		return statefulFSM;
	}

}
