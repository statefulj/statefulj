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
package org.statefulj.common.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectionUtilsTest {
	
	@Target({ElementType.FIELD, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface Bar {
		
		public String value() default "";
		
	}	
	
	@Test
	public void testFieldFromGetter() {
		
		class Foo {

			String foo = "boo";

			@Bar
			public String getFoo() {
				return foo;
			}

			public void setFoo(String foo) {
				this.foo = foo;
			}
		}
		
		Field foo = ReflectionUtils.getReferencedField(Foo.class, Bar.class);
		assertNotNull(foo);
		assertEquals("foo", foo.getName());
	}
}
