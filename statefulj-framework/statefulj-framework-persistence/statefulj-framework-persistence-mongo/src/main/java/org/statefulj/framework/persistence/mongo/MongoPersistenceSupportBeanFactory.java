package org.statefulj.framework.persistence.mongo;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.statefulj.framework.core.model.impl.CrudRepositoryFinderImpl;
import org.statefulj.framework.core.model.impl.FSMHarnessImpl;
import org.statefulj.framework.core.model.impl.FactoryImpl;
import org.statefulj.framework.core.springdata.PersistenceSupportBeanFactory;
import org.statefulj.persistence.mongo.MongoPersister;

public class MongoPersistenceSupportBeanFactory implements PersistenceSupportBeanFactory {

	@Override
	public Class<?> getKey() {
		return MongoRepositoryFactoryBean.class;
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
			String stateFieldName,
			List<RuntimeBeanReference> stateBeans) {
		BeanDefinition persisterBean = BeanDefinitionBuilder
				.genericBeanDefinition(MongoPersister.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, stateBeans);
		args.addIndexedArgumentValue(1, stateFieldName);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(startStateId));
		args.addIndexedArgumentValue(3, statefulClass);
		return persisterBean;
	}

	
	@Override
	public BeanDefinition buildFSMHarnessBean(
			Class<?> statefulClass,
			String fsmBeanId,
			String factoryId, 
			String finderId) {

		BeanDefinition fsmHarness = BeanDefinitionBuilder
				.genericBeanDefinition(FSMHarnessImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = fsmHarness.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
		args.addIndexedArgumentValue(1, statefulClass);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
		args.addIndexedArgumentValue(3, new RuntimeBeanReference(finderId));
		return fsmHarness;
	}

}
