package org.statefulj.webapp.messaging;

public interface LoanApplicationProducer {

	public void onApplicationSubmitted(LoanApplication msg);

}
