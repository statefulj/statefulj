package org.statefulj.framework;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAlias(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void registerAlias(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAlias(String arg0) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
		return registry.get(beanName);
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		// TODO Auto-generated method stub
		return registry.containsKey(beanName);
	}

	@Override
	public String[] getBeanDefinitionNames() {
		// TODO Auto-generated method stub
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(registry.keySet());
		return keys.toArray(new String[]{});
	}

	@Override
	public int getBeanDefinitionCount() {
		// TODO Auto-generated method stub
		return registry.size();
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		// TODO Auto-generated method stub
		return false;
	}

}
