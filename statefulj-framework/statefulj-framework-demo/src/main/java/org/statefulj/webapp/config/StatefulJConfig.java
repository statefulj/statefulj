package org.statefulj.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.statefulj.framework.core.StatefulFactory;

@Configuration
public class StatefulJConfig {
	
	@Bean
	public StatefulFactory statefulJFactory() {
		return new StatefulFactory();
	}

}
