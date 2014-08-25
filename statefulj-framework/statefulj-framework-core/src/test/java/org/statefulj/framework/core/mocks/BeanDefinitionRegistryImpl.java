package org.statefulj.framework.core.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class BeanDefinitionRegistryImpl implements BeanDefinitionRegistry {
	
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
