<html>
	<head>
		<title>StatefulJ Trust Bank Demo</title>
	</head>
	<body>
		<div class="splash">
			<div class="container">
				<div class="row">
					<div class="col-lg-12">
						<h1>StatefulJ Trust Bank</h1>
						<p class="lead">A Banking Demo built on StatefulJ</p>
					</div>
				</div>
			</div>
		</div>
		<div class="forms">
			<div class="container">
				<div class="row">
					<div class="col-md-offset-1 col-md-5">
						<div class="well bs-component">
							<%@ include file="/WEB-INF/views/forms/registration.jsp" %> 
						</div>
					</div>
					<div class="col-md-5">
						<div class="well bs-component">
							<%@ include file="/WEB-INF/views/forms/login.jsp" %> 
						</div>
					</div>
				</div>
			</div>
		</div>
	</body>
</html>
