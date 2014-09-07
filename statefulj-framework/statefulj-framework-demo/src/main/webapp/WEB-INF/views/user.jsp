<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url var="createSavingsAccountUrl" value="/accounts/savings" />
<c:url var="createCheckingAccountUrl" value="/accounts/checking" />
<c:url var="createLoanAccountUrl" value="/accounts/loan" />
<c:url var="deleteUserUrl" value="/user/delete" />
<html>
	<head>
		<title>User Page</title>
	</head>
	<body>
		<div class="row">
			<div class="col-md-offset-1 col-md-10">
				<h2>${user.firstName} ${user.lastName}</h2>
			</div>
		</div>
		<div class="row">
			<div class="col-md-offset-1 col-md-10">
				<h3>Accounts:</h3>
				<c:if test="${empty user.accounts}">
					<div class="alert alert-info">
						You need to set up some accounts
					</div>
				</c:if>
				<c:forEach items="${user.accounts}" var="account">
					<li><a href="accounts/${account.id}">${account}</a></li>
				</c:forEach>
				<div class="account-create">
					<a href="${createSavingsAccountUrl}" class="btn btn-default">+ Savings Account</a>
					<a href="${createCheckingAccountUrl}" class="btn btn-default">+ Checking Account</a>
					<a href="${createLoanAccountUrl}" class="btn btn-default">+ Loan Account</a>
				</div>
				<a href="${deleteUserUrl}" class="btn btn-default">Delete User</a>
			</div>
		</div>
	</body>
</html>
