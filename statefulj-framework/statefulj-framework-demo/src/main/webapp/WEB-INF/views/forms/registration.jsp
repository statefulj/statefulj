<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url value="/registration" var="registrationUrl"/>
<c:if test="${not empty message}">
	<div class="alert alert-warning">
	  ${message}.
	</div>
</c:if>
<form action="${registrationUrl}" method="post" class="form-horizontal">
	<fieldset>
		<legend>Register</legend>
		<div class="form-group">
			<label for="firstName" class="col-lg-2 control-label">First Name</label>
			<div class="col-lg-10">
				<input type="text" class="form-control" id="firstName"
					name="firstName" placeholder="First Name" value="${reg.firstName}" />
			</div>
		</div>
		<div class="form-group">
			<label for="lastName" class="col-lg-2 control-label">Last Name</label>
			<div class="col-lg-10">
				<input type="text" class="form-control" id="lastName"
					name="lastName" placeholder="Last Name" value="${reg.lastName}" />
			</div>
		</div>
		<div class="form-group">
			<label for="email" class="col-lg-2 control-label">Email</label>
			<div class="col-lg-10">
				<input type="text" class="form-control" id="email"
					name="email" placeholder="Email" value="${reg.email}" />
			</div>
		</div>
		<div class="form-group">
			<label for="password" class="col-lg-2 control-label">Password</label>
			<div class="col-lg-10">
				<input type="password" class="form-control" id="password"
					name="password" placeholder="Password">
			</div>
		</div>
		<div class="form-group">
			<label for="passwordConfirmation" class="col-lg-2 control-label">Confirm Password</label>
			<div class="col-lg-10">
				<input type="password" class="form-control" id="passwordConfirmation"
					name="passwordConfirmation" placeholder="Confirm Password">
			</div>
		</div>
		<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
		<button type="submit" class="btn btn-default center-block">Register</button>
	</fieldset>
</form>
