<%@ page language="java" contentType="text/html; charset=US-ASCII"
	pageEncoding="US-ASCII"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<c:set var="req" value="${pageContext.request}" />
<c:set var="url">${req.requestURL}</c:set>
<c:set var="uri" value="${req.requestURI}" />
<c:set var="logoutUrl" value="/logout" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>User Page</title>
<base
	href="${fn:substring(url, 0, fn:length(url) - fn:length(uri))}${req.contextPath}/">
</head>
<body>
	user.id=${user.id}
	<br /> user.state=${user.state}
	<br /> event=${event}
	<br />
	<c:forEach items="${user.accounts}" var="account">
		<li><a href="accounts/${account.id}">${account}</a></li>
	</c:forEach>
	<a href="logout">logout</a>
	<form action="accounts/checking" method="post">
		<button type="submit">Add Checking Account</button>
		<input type="hidden" name="${_csrf.parameterName}"
			value="${_csrf.token}" />
	</form>
	<form action="accounts/savings" method="post">
		<button type="submit">Add Savings Account</button>
		<input type="hidden" name="${_csrf.parameterName}"
			value="${_csrf.token}" />
	</form>
	<form action="accounts/loan" method="post">
		<button type="submit">Add Loan Account</button>
		<input type="hidden" name="${_csrf.parameterName}"
			value="${_csrf.token}" />
	</form>
	<a href="user/delete">Delete User</a>
</body>
</html>
