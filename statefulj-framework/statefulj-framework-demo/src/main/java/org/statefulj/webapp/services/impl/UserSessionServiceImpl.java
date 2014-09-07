package org.statefulj.webapp.services.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.statefulj.framework.core.model.Finder;
import org.statefulj.webapp.model.User;
import org.statefulj.webapp.repo.UserRepository;
import org.statefulj.webapp.services.UserSessionService;

@Service(value="userSessionService")
@Transactional
public class UserSessionServiceImpl implements UserSessionService, UserDetailsService, Finder<User, HttpServletRequest> {
	
	@Resource
	UserRepository userRepo;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		final User user = userRepo.findByEmail(username);
		
		if (user == null) {
			throw new UsernameNotFoundException("Unable to locate " + username);
		}
		
		return getDetails(user);
	}

	@Override
	public User find(Class<User> clazz, String event, HttpServletRequest context) {
		return findLoggedInUser();
	}

	@Override
	public User find(Class<User> clazz, Object id, String event, HttpServletRequest context) {
		return null;
	}

	@Override
	public User findLoggedInUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String name = auth.getName(); //get logged in username
		return userRepo.findByEmail(name);
	}

	@Override
	public void login(HttpSession session, User user) {
      UserDetails userDetails = this.getDetails(user);
      UsernamePasswordAuthenticationToken auth = 
    		  new UsernamePasswordAuthenticationToken(
    				  userDetails, 
    				  user.getPassword(), 
    				  userDetails.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(auth);  
      session.setAttribute(
    		  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
    		  SecurityContextHolder.getContext());  
	}
	
	private UserDetails getDetails(final User user) {
		final List<GrantedAuthority> authorities = new LinkedList<GrantedAuthority>();
		authorities.add(new GrantedAuthority() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getAuthority() {
				return "USER";
			}
		});
		
		UserDetails details = new UserDetails() {
			private static final long serialVersionUID = 1L;
			String userName = user.getEmail();
			String password = user.getPassword();
			boolean isEnabled = !User.DELETED.equals(user.getState());
			
			@Override
			public boolean isEnabled() {
				return isEnabled;
			}
			
			@Override
			public boolean isCredentialsNonExpired() {
				return true;
			}
			
			@Override
			public boolean isAccountNonLocked() {
				return true;
			}
			
			@Override
			public boolean isAccountNonExpired() {
				return true;
			}
			
			@Override
			public String getUsername() {
				return userName;
			}
			
			@Override
			public String getPassword() {
				return password;
			}
			
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return authorities;
			}
		};
		return details;
	}

}
