<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib prefix="c" 
           uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="req" value="${pageContext.request}" />
<c:set var="url">${req.requestURL}</c:set>
<c:set var="uri" value="${req.requestURI}" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
	<title>Any</title>
	<base href="${fn:substring(url, 0, fn:length(url) - fn:length(uri))}${req.contextPath}/">
</head>
<body> 
	user.id=${user.id}<br/>
	user.state=${user.state}<br/>
	event=${event}<br/>
	<a href="${user.id}/next.mvc">next</a><br/>
	<a href="${user.id}/whatever.mvc">whatever</a><br/>
	<a href="${user.id}/any.mvc">any</a><br/>
	<form method="post" action="${user.id}/post.mvc">
		<input type="submit" name="submit" value="submit" />
	</form>
</body>
</html>
