<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Confirmation</title>
</head>
<body>
    <h1>${message}</h1>
    <p>Nom : ${eleve.nom}</p>
    <p>Âge : ${eleve.age}</p>
    <p><a href="${pageContext.request.contextPath}/eleve/form">Nouvel élève</a></p>
</body>
</html>