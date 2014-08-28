package org.statefulj.framework.core.mocks;

import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;


@SuppressWarnings("rawtypes")
public class MockRepositoryFactoryBeanSupport extends RepositoryFactoryBeanSupport {

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		return null;
	}

}
