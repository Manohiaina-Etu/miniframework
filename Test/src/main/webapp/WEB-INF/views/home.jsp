<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Accueil - Mini Framework MVC</title>
</head>
<body>
    <h1>${message}</h1>
    <ul>
        <li><a href="${pageContext.request.contextPath}/eleve/form">Formulaire élève</a></li>
        <li><a href="${pageContext.request.contextPath}/eleve/5">Détail de l'élève 5 (@PathVariable)</a></li>
    </ul>
</body>
</html>