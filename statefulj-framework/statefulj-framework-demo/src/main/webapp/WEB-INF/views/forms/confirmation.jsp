<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url value="/user/confirmation" var="confirmationUrl"/>
<c:if test="${not empty message}">
	<div class="alert alert-warning">
	  ${message}.
	</div>
</c:if>
<form action="${confirmationUrl}" method="post" class="form-horizontal">
	<fieldset>
		<legend>Confirmation</legend>
		<div class="form-group">
			<label for="token" class="col-lg-2 control-label">Confirmation Token</label>
			<div class="col-lg-10">
				<input type="text" class="form-control" id="token" name="token" placeholder="Confirmation Token" />
			</div>
		</div>
		<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
		<button type="submit" class="btn btn-default center-block">Confirm</button>
	</fieldset>
</form>
