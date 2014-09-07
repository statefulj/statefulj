<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:url value="/logout" var="logoutUrl"/>
<html>
<head>
	<title>Confirmation Page</title>
</head>
<body> 
	confirmation token=${user.token}<br/>
	<c:url value="/user/confirmation" var="confirmationUrl"/>
<form action="${confirmationUrl}" method="post">
    <p>
        <label for="confirmation">Enter your confirmation token</label>
        <input type="text" id="token" name="token"/>	
    </p>
    <input type="hidden"                        
        name="${_csrf.parameterName}"
        value="${_csrf.token}"/>
    <button type="submit" class="btn btn-default">Confirm</button>
</form>
<a href="${logoutUrl}">logout</a>
</body>
</html>
