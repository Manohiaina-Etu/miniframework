<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Mini Framework MVC - Sprint 0</title>
</head>
<body>
    <h1>Sprint 0 : déploiement OK</h1>
    <p>Serveur : <%= application.getServerInfo() %></p>
    <p>Servlet API : <%= application.getMajorVersion() %>.<%= application.getMinorVersion() %></p>
</body>
</html>