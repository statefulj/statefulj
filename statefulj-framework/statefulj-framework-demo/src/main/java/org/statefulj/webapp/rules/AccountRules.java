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
package org.statefulj.webapp.rules;

import java.util.Calendar;
import java.util.Random;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.impl.DefaultMessage;
import org.apache.commons.lang.math.RandomUtils;
import org.statefulj.webapp.messaging.AccountApplication;
import org.statefulj.webapp.model.LoanAccount;

import static org.apache.camel.ExchangePattern.*;

public class AccountRules {
	static Random random = new Random(Calendar.getInstance().getTimeInMillis());
	
	// Public Queues
	//
	public static final String REVIEW_APPLICATION = "vm:account.application";
	public static final String ACCOUNT_APPROVED = "vm:account.approved";
	public static final String ACCOUNT_REJECTED = "vm:account.rejected";
	public static final String LOAN_APPROVED = "vm:account.loan.approved";
	public static final String LOAN_REJECTED = "vm:account.loan.rejected";
	
	// Private Queues
	//
	private static final String REVIEW_LOAN = "vm:loan.application.review";

	// Determines whether the application is a loan
	//
	private static Predicate isLoan = new Predicate() {
		@Override
		public boolean matches(Exchange exchange) {
			return getApplication(exchange).getType().equals(LoanAccount.TYPE);
		}
	};
	
	// Determines whether a loan is approved
	//
	private static Predicate loanApproved = new Predicate() {
		@Override
		public boolean matches(Exchange exchange) {
			return getApplication(exchange).isApproved();
		}
	};
	
	// Randomly approve a loan.  Still better than most lending standards...
	//
	private static Processor reviewApplication = new Processor() {
		
		@Override
		public void process(Exchange exchange) throws Exception {
			AccountApplication msg = getApplication(exchange);
			boolean approved = RandomUtils.nextBoolean(random);
			msg.setApproved(approved);
			msg.setReason((approved) 
					? "You're approved cause we like you" 
					: "Sorry, we can't approve the loan - you are a deadbeat");
		}
	};

	public static RouteBuilder routingRules() {
        return new RouteBuilder() {
            public void configure() {
            	
            	// When there is a loan application, route the review queue; otherwise,
            	// automatically approve
            	//
                from(REVIEW_APPLICATION).
                	choice().
                		when(isLoan).
                			setExchangePattern(InOnly). // Process asynchronously
                			to(REVIEW_LOAN).
                		otherwise().
                			setExchangePattern(InOut). // Process synchronously
                			to(ACCOUNT_APPROVED);
                
            	// It takes 5 seconds to review... very slow - bankers' hours...
            	//
                from(REVIEW_LOAN).
                	delay(5 * 1000).
                	process(reviewApplication).
                	choice().
                    	when(loanApproved).
                        	to(LOAN_APPROVED).
                        otherwise().
                        	to(LOAN_REJECTED);
           }
        };
	}

    private static AccountApplication getApplication(Exchange exchange) {
    	return getMessage(exchange, AccountApplication.class);
    }
    
    @SuppressWarnings("unchecked")
	private static <T> T getMessage(Exchange exchange, Class<T> returnType) {
    	return (T)(exchange.getIn(DefaultMessage.class).getBody(BeanInvocation.class).getArgs()[0]);
    }

}
