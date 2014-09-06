<%@ page language="java" contentType="text/html; charset=US-ASCII"
	pageEncoding="US-ASCII"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<c:set var="req" value="${pageContext.request}" />
<c:set var="url">${req.requestURL}</c:set>
<c:set var="uri" value="${req.requestURI}" />
<c:url var="registrationUrl" value="/registration" />
<c:url var="createAccountUrl" value="/accounts"/>
<html>
<head>
<title>StatefulJ Trust</title>
</head>
<body>
	<a href="${registrationUrl}">Register</a>
	<a href="${loginUrl}">Login</a>
</body>
</html>
