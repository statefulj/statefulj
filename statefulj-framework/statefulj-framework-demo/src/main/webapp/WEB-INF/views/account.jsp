<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib prefix="c" 
           uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="req" value="${pageContext.request}" />
<c:set var="url">${req.requestURL}</c:set>
<c:set var="uri" value="${req.requestURI}" />
<c:set var="logoutUrl" value="/logout" />
<html>
<head>
	<title>Account ${account.id} Page</title>
</head>
<body> 
	account.id=${account.id}<br/>
	<a href="user">back &lt;&lt;</a>
</body>
</html>
