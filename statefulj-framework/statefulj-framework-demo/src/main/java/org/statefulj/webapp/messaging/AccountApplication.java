package org.statefulj.webapp.messaging;

import javax.persistence.Id;

public class AccountApplication {
	
	@Id
	private Long accountId;
	
	private boolean approved;
	
	private String reason;
	
	private String type;

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
