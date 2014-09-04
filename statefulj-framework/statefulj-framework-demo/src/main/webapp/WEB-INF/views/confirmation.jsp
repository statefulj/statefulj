<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib prefix="c" 
           uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="req" value="${pageContext.request}" />
<c:set var="url">${req.requestURL}</c:set>
<c:set var="uri" value="${req.requestURI}" />
<c:set var="logoutUrl" value="/logout" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
	<title>Any</title>
	<base href="${fn:substring(url, 0, fn:length(url) - fn:length(uri))}${req.contextPath}/">
</head>
<body> 
	confirmation token=${user.token}<br/>
<c:url value="/user/confirmation" var="confirmationUrl"/>
<c:url value="/logout" var="logoutUrl"/>
<form action="${confirmationUrl}" method="post">
    <p>
        <label for="confirmation">Enter your confirmation token</label>
        <input type="text" id="token" name="token"/>	
    </p>
    <input type="hidden"                        
        name="${_csrf.parameterName}"
        value="${_csrf.token}"/>
    <button type="submit" class="btn">Confirm</button>
</form>
<a href="${logoutUrl}">logout</a>
</body>
</html>
