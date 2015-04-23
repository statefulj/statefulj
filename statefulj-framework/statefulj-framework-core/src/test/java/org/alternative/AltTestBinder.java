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
package org.alternative;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import org.statefulj.framework.core.mocks.MockProxy;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.ReferenceFactory;

public class AltTestBinder implements EndpointBinder {

	@Override
	public String getKey() {
		return "test";
	}

	@Override
	public Class<?> bindEndpoints(
			String beanId,
			Class<?> stateControllerClass,
			Class<?> idType,
			boolean isDomainEntity,
			Map<String, Method> eventMapping, 
			ReferenceFactory refFactory)
			throws CannotCompileException, NotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {

		return MockProxy.class;
	}

}
