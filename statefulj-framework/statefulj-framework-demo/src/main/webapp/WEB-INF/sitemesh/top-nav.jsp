<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<c:url value="/" var="homeUrl" />
<c:url value="/logout" var="logoutUrl" />
<div class="navbar navbar-default">
	<div class="container">
		<div class="navbar-header">
			<a class="navbar-brand" href="${homeUrl}">StatefulJ Trust Bank</a>
		</div>
		<sec:authorize access="isAuthenticated()"> 
			<ul class="nav navbar-nav navbar-right">
				<li><a href="${logoutUrl}">Logout</a></li>
			</ul>
		</sec:authorize>
	</div>
</div>