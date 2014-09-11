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
