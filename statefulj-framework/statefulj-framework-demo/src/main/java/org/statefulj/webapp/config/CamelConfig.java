package org.statefulj.webapp.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.javaconfig.SingleRouteCamelConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.statefulj.webapp.rules.AccountRules;

/**
 * A simple example router from a file system to an ActiveMQ queue and then to a file system
 *
 * @version 
 */
@Configuration 
public class CamelConfig extends SingleRouteCamelConfiguration {
     
    @Bean
    @Override
    public RouteBuilder route() {
    	return AccountRules.routingRules();
    }
}