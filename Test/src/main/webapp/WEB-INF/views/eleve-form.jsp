<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Nouvel élève</title>
</head>
<body>
    <h1>Nouvel élève</h1>
    <form method="post" action="${pageContext.request.contextPath}/eleve/save">
        <p><label>Nom : <input type="text" name="nom"></label></p>
        <p><label>Âge : <input type="text" name="age"></label></p>
        <p><button type="submit">Enregistrer</button></p>
    </form>
</body>
</html>