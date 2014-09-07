<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:url value="/logout" var="logoutUrl"/>
<html>
	<head>
		<title>Confirmation Page</title>
	</head>
	<body> 
		<div class="row">
			<div class="col-md-offset-3 col-md-6">
				<div class="well bs-component">
					<div class="alert alert-info">
					  Your confirmation code is <strong>${user.token}</strong>
					</div>
					<%@ include file="/WEB-INF/views/forms/confirmation.jsp"%>
				</div>
			</div>
		</div>
	</body>
</html>
