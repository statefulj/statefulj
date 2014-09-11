package org.statefulj.webapp.config;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.spring.javaconfig.SingleRouteCamelConfiguration;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.statefulj.webapp.messaging.LoanReviewCompleteMessage;

/**
 * A simple example router from a file system to an ActiveMQ queue and then to a file system
 *
 * @version 
 */
@Configuration 
@ComponentScan("org.statefulj.webapp")
public class CamelConfig extends SingleRouteCamelConfiguration {
     
    @Bean
    @Override
    public RouteBuilder route() {
    	
    	// Determines whether a loan is approved
    	//
    	final Predicate loanApproved = new Predicate() {
			@Override
			public boolean matches(Exchange exchange) {
				return getMessage(exchange, LoanReviewCompleteMessage.class).isApproved();
			}
		};
		
		// Randomly approve a loan.  Still better than most lending standards...
		//
		final Processor reviewApplication = new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
				LoanReviewCompleteMessage msg = getMessage(exchange, LoanReviewCompleteMessage.class);
				boolean approved = RandomUtils.nextBoolean();
				msg.setApproved(approved);
				msg.setReason((approved) 
						? "You're approved cause we like you" 
						: "Sorry, we can't approve the loan - you are a deadbeat");
			}
		};
    	
        return new RouteBuilder() {
            public void configure() {
            	
            	// When there is a loan application, route the review queue
            	//
                from("vm:loan.application").
                	setExchangePattern(ExchangePattern.InOnly).
                	to("vm:loan.application.review");
                
            	// It takes 10 seconds to review... very slow
            	//
                from("vm:loan.application.review").
                	delay(10 * 1000).
                	process(reviewApplication).
                	choice().
                    	when(loanApproved).
                        	to("vm:loan.approved").
                        otherwise().
                        	to("vm:loan.rejected");
           }
        };
    }
    
    @SuppressWarnings("unchecked")
	private <T> T getMessage(Exchange exchange, Class<T> returnType) {
    	return (T)(exchange.getIn(DefaultMessage.class).getBody(BeanInvocation.class).getArgs()[0]);
    }
}