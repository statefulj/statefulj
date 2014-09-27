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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class MockBeanDefinitionRegistryImpl implements BeanDefinitionRegistry {
	
	private Map<String, BeanDefinition> registry = new HashMap<String, BeanDefinition>();

	@Override
	public String[] getAliases(String arg0) {
		return null;
	}

	@Override
	public boolean isAlias(String arg0) {
		return false;
	}

	@Override
	public void registerAlias(String arg0, String arg1) {
	}

	@Override
	public void removeAlias(String arg0) {
	}

	@Override
	public void registerBeanDefinition(String beanName,
			BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
		registry.put(beanName, beanDefinition);
	}

	@Override
	public void removeBeanDefinition(String beanName)
			throws NoSuchBeanDefinitionException {
		registry.remove(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName)
			throws NoSuchBeanDefinitionException {
		return registry.get(beanName);
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return registry.containsKey(beanName);
	}

	@Override
	public String[] getBeanDefinitionNames() {
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(registry.keySet());
		return keys.toArray(new String[]{});
	}

	@Override
	public int getBeanDefinitionCount() {
		return registry.size();
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		return false;
	}

}
