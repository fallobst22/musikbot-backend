<% 
	if(request.getAttribute("worked") == null) {
		response.sendError(404);
		return;
	}
%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="de.elite12.musikbot.server.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="org.springframework.web.util.HtmlUtils"%>
<%@ page import="java.util.UUID"%>
<% 
	ResultSet rs = (ResultSet)request.getAttribute("result");
%>
<!DOCTYPE html>
<html lang="de">
	<head>
		<meta charset="UTF-8">
		<meta name="ROBOTS" content="INDEX, FOLLOW">
		<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0">
		<meta name="theme-color" content="#555555">
		<link rel="stylesheet" type="text/css" href="/res/styles/mobile.css" />
		<link rel="shortcut icon" href="/res/favicon.ico">
		<script type="text/javascript">
		  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
		  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
		  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
		  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
		
		  <%if (session.getAttribute("user") != null) {%>
			ga('create', 'UA-60228333-1', { 'userId': '<%=((User) session.getAttribute("user")).hashCode()%>'});
		<%} else {%>
			ga('create', 'UA-60228333-1', 'auto');
		<%}%>
			ga('send', 'pageview');
		</script>
		<script async src="/res/cookiechoices.js"></script>
		<link rel="alternate" type="application/rss+xml" title="RSS"
			href="https://musikbot.elite12.de/feed/" />
	</head>
	<body>
		<script>
		  document.addEventListener('DOMContentLoaded', function(event) {
		    cookieChoices.showCookieConsentBar('Cookies erleichtern die Bereitstellung unserer Dienste. Mit der Nutzung unserer Dienste erklären Sie sich damit einverstanden, dass wir Cookies verwenden.',
		      'Schließen', 'Weitere Informationen', '/impressum/');
		  });
		</script>
		<div class="currentsong"><%= HtmlUtils.htmlEscape(((Controller)request.getAttribute("control")).getSongtitle())%></div>
		<div class="playlist">
			<table>
				<thead>
					<tr>
						<th>ID</th>
						<th>Autor</th>
						<th>Titel</th>
					</tr>
				</thead>
				<tbody>
					<% 
					int dauer = 0;
					while(rs.next()) { 
					dauer += rs.getInt("SONG_DAUER");
					%>
					<tr class="songentry" id='song_<%= rs.getInt("SONG_ID") %>'>
						<td><%= rs.getInt("SONG_ID") %></td>
						<td>
							<% 
							try {
								UUID id = UUID.fromString(rs.getString("AUTOR")); 
								%>Gast<%
							}
							catch (IllegalArgumentException e) {%> <%= rs.getString("AUTOR") %>
							<%} %>
						</td>
						<td><a href="<%= rs.getString("SONG_LINK") %>"><%= rs.getString("SONG_NAME").length() > 35 ? HtmlUtils.htmlEscape(rs.getString("SONG_NAME").substring(0, 35)) + "..." : HtmlUtils.htmlEscape(rs.getString("SONG_NAME")) %></a></td>
					</tr>
					<% } %>
					<tr>
						<td colspan="3">
							<form method="post" action="/">
								<input type="text" name="link" />
								<input id="submit" type="submit" name="Button" value="Abschicken" />
								<input type="hidden" name="action" value="addsong" />
							</form>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</body>
</html>