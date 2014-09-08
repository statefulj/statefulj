<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:url value="${createAccountUri}" var="createAccountUrl"/>
<c:if test="${not empty message}">
	<div class="alert alert-warning">
	  ${message}.
	</div>
</c:if>
<form action="${createAccountUrl}" method="post" class="form-horizontal">
	<fieldset>
		<legend>Create ${typeTitle} Account:</legend>
		<div class="form-group">
			<label for="amount" class="col-lg-2 control-label">Amount</label>
			<div class="col-lg-10">
				<input type="text" class="form-control" id="amount" name="amount" placeholder="Amount" />
			</div>
		</div>
		<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
		<input type="hidden" name="type" value="${type}" />
		<button type="submit" class="btn btn-default center-block">Create</button>
	</fieldset>
</form>
