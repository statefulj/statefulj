<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url var="createCheckingAccount" value="/accounts/checkings" />
<c:url var="createLoanAccountUrl" value="/accounts/loans" />
<c:url var="createSavingsAccountUrl" value="/accounts/savings" />
<c:url var="deleteUserUrl" value="/user/delete" />
<html>
	<head>
		<title>Create ${typeTitle} Account</title>
	</head>
	<body>
		<div class="row">
			<div class="col-md-offset-1 col-md-10">
				<div class="well bs-component">
					<%@ include file="/WEB-INF/views/forms/account.jsp"%>
				</div>
			</div>
		</div>
	</body>
</html>
