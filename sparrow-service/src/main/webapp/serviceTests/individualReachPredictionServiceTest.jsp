<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page contentType="text/html;charset=ISO-8859-1"%>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>Individual Reach Prediction Service Test</title>
    <link rel="icon" href="../favicon.ico" />
  </head>
  <body>

		<form action="../sp_indivReachPredict/formpost" method="get" enctype="application/x-www-form-urlencoded">
			<fieldset title="Individual Reach Prediction Service ()">
				<table>
					<tr>
						<td><label for="context-id">context-id*</label></td>
						<td><input type="text" name="context-id"/><em>if you submit a context-id, make sure your context-id is defined first, via the <a href="http://localhost:8088/sparrow_mv/serviceTests/predictContextServiceTest.jsp">Prediction Context Service</a>!</em></td>
					</tr>
					<tr>
						<td><label for="model">model</label></td>
						<td><input type="text" name="model"/></td>
					</tr>

					<tr>
						<td><label for="reachID">reach identifier</label></td>
						<td><input type="text" name="reachID"/></td>
					</tr>
				</table>
				<b>* </b><i>optional</i>
				<br />
				<input type="submit" value="Submit"/>
			</fieldset>
		</form>

	</body>
</html>