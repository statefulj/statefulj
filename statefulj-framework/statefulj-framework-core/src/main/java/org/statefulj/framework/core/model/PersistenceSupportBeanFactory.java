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
package org.statefulj.framework.core.model;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;

public interface PersistenceSupportBeanFactory {

	/**
	 * Associate this PersistenceSupportBeanFactory with a Spring Data Repo Factory
	 * 
	 * @return Class of the Repo Factory
	 */
	Class<?> getKey(); 
	
	Class<?> getIdType(); 

	Class<? extends Annotation> getIdAnnotationType();

	BeanDefinition buildFactoryBean(Class<?> statefulClass);
	
	BeanDefinition buildFinderBean(String repoBeanId);
	
	BeanDefinition buildPersisterBean(
			Class<?> statefulClass,
			String repoBeanId,
			BeanDefinition repoBeanDefinitionFactory,
			String stateFieldName,
			String startStateId, 
			List<RuntimeBeanReference> stateBeans);

	BeanDefinition buildFSMHarnessBean(
			Class<?> statefulClass, 
			String fsmBeanId,
			String factoryId,
			String finderId,
			BeanDefinition repoBeanDefinitionFactory);
}
