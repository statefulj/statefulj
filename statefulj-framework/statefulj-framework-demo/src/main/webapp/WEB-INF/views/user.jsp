<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url var="createAccountUrl" value="/accounts"/>
<html>
<head>
	<title>User Page</title>
</head>
<body>
	user.id=${user.id}
	<br /> user.state=${user.state}
	<br /> event=${event}
	<br />
	<c:forEach items="${user.accounts}" var="account">
		<li><a href="accounts/${account.id}">${account}</a></li>
	</c:forEach>
	<form action="${createAccountUrl}" method="post">
		<input type="hidden" name="type"
			value="checking" />
		<input type="hidden" name="${_csrf.parameterName}"
			value="${_csrf.token}" />
		<button type="submit" class="btn btn-default">Add Checking Account</button>
	</form>
	<form action="${createAccountUrl}" method="post">
		<input type="hidden" name="type"
			value="savings" />
		<input type="hidden" name="${_csrf.parameterName}"
			value="${_csrf.token}" />
		<button type="submit" class="btn btn-default">Add Savings Account</button>
	</form>
	<form action="${createAccountUrl}" method="post">
		<input type="hidden" name="type"
			value="loan" />
		<input type="hidden" name="${_csrf.parameterName}"
			value="${_csrf.token}" />
		<button type="submit" class="btn btn-default">Add Loan Account</button>
	</form>
	<a href="${deleteUrl}" class="btn btn-default">Delete User</a>
</body>
</html>
