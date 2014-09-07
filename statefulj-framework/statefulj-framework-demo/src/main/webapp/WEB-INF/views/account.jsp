<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url value="/user" var="userUrl" />
<html>
	<head>
		<title>Account ${account.id} Page</title>
	</head>
	<body>
		<div class="row">
			<div class="col-md-offset-1 col-md-10">
				account.id=${account.id}<br /> <a href="${userUrl}">back &lt;&lt;</a>
			</div>
		</div>
	</body>
</html>
