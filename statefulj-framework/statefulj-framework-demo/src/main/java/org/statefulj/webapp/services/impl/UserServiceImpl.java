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
package org.statefulj.webapp.services.impl;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;
import org.statefulj.webapp.services.UserService;

@Service(value="userService")
@Transactional
public class UserServiceImpl implements UserService {
	
	@Resource
	UserRepository userRepo;

	@PersistenceContext
	EntityManager entityManager;
	
	@Override
	public User findById(Long id) {
		return userRepo.findOne(id);
	}

	@Override
	public User findByEmail(String email) {
		return userRepo.findByEmail(email);
	}
	
	@Override
	public void save(User user) {
		userRepo.save(user);
		entityManager.flush();
	}

}
