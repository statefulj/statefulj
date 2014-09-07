<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url value="/login" var="loginUrl"/>
<c:if test="${param.error != null}">        
	<div class="alert alert-warning">
	  Invalid Email or Password
	</div>
</c:if>
<form action="${loginUrl}" method="post" class="form-horizontal">
	<fieldset>
		<legend>Login</legend>
		<div class="form-group">
			<label for="username" class="col-lg-2 control-label">Email</label>
			<div class="col-lg-10">
				<input type="text" class="form-control" id="username"
					name="username" placeholder="Email">
			</div>
		</div>
		<div class="form-group">
				<label for="password" class="col-lg-2 control-label">Password</label>
				<div class="col-lg-10">
				<input type="password" class="form-control" id="password"
					name="password" placeholder="Password">
			</div>
		</div>
		<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
		<button type="submit" class="btn btn-default center-block">Login</button>
	</fieldset>
</form>
