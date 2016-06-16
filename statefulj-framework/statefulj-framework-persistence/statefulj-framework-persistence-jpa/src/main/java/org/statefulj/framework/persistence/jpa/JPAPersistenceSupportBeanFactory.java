/***
 * 
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.statefulj.framework.persistence.jpa;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.persistence.Id;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;
import org.statefulj.framework.core.model.impl.CrudRepositoryFinderImpl;
import org.statefulj.framework.core.model.impl.FactoryImpl;
import org.statefulj.persistence.jpa.JPAPerister;

public class JPAPersistenceSupportBeanFactory implements PersistenceSupportBeanFactory {

	@Override
	public Class<?> getKey() {
		return JpaRepositoryFactoryBean.class;
	}

	@Override
	public Class<?> getIdType() {
		return Long.class;
	}

	@Override
	public Class<? extends Annotation> getIdAnnotationType() {
		return Id.class;
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
	public BeanDefinition buildPersisterBean(
			Class<?> statefulClass,
			String repoBeanId,
			BeanDefinition repoBeanDefinitionFactory,
			String stateFieldName,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans) {
		BeanDefinition entityMgr = (BeanDefinition)repoBeanDefinitionFactory.getPropertyValues().getPropertyValue("entityManager").getValue();
		String tmId = (String)repoBeanDefinitionFactory.getPropertyValues().getPropertyValue("transactionManager").getValue();
		BeanDefinition persisterBean = BeanDefinitionBuilder
				.genericBeanDefinition(JPAPerister.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = persisterBean.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, stateBeans);
		args.addIndexedArgumentValue(1, stateFieldName);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(startStateId));
		args.addIndexedArgumentValue(3, statefulClass);
		args.addIndexedArgumentValue(4, entityMgr);
		args.addIndexedArgumentValue(5, new RuntimeBeanReference(tmId));
		return persisterBean;
	}
	
	@Override
	public BeanDefinition buildFSMHarnessBean(
			Class<?> statefulClass,
			String fsmBeanId,
			String factoryId, 
			String finderId,
			BeanDefinition repoBeanDefinitionFactory) {
		String tmId = (String)repoBeanDefinitionFactory.getPropertyValues().getPropertyValue("transactionManager").getValue();
		
		BeanDefinition fsmHarness = BeanDefinitionBuilder
				.genericBeanDefinition(JPAFSMHarnessImpl.class)
				.getBeanDefinition();
		ConstructorArgumentValues args = fsmHarness.getConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
		args.addIndexedArgumentValue(1, statefulClass);
		args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
		args.addIndexedArgumentValue(3, new RuntimeBeanReference(finderId));
		args.addIndexedArgumentValue(4, new RuntimeBeanReference(tmId));
		return fsmHarness;
	}
}
