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
package org.statefulj.framework.core.mocks;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;

public class MockPersistenceSupportBeanFactory implements
		PersistenceSupportBeanFactory {

	@Override
	public Class<?> getKey() {
		return MockRepositoryFactoryBeanSupport.class;
	}

	@Override
	public Class<?> getIdType() {
		return Object.class;
	}

	/* (non-Javadoc)
	 * @see org.statefulj.framework.core.model.PersistenceSupportBeanFactory#getIdAnnotationType()
	 */
	@Override
	public Class<? extends Annotation> getIdAnnotationType() {
		return null;
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
	public BeanDefinition buildPersisterBean(
			Class<?> statefulClass,
			String repoBeanId,
			String stateFieldName,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans,
			BeanDefinition repoBeanDefinitionFactory) {
		return mockDef();
	}

	@Override
	public BeanDefinition buildFSMHarnessBean(
			Class<?> statefulClass,
			String fsmBeanId, 
			String factoryId, 
			String finderId, 
			BeanDefinition repoBeanDefinitionFactory) {
		return mockDef();
	}
	
	private BeanDefinition mockDef() {
		return BeanDefinitionBuilder
				.genericBeanDefinition(Object.class)
				.getBeanDefinition();
	}
}
