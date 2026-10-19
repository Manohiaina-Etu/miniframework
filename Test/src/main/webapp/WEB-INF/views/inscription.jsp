<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Inscription</title>
</head>
<body>
    <h1>Simple file</h1>
    <form method="post"  action="${pageContext.request.contextPath}/inscription/save_eleve" enctype="multipart/form-data">
        <p><label>Nom : <input type="text" name="eleve.nom"></label></p>
        <p><label>Âge : <input type="text" name="eleve.age"></label></p>
        <p><label>Document : <input type="file" name="fichier" multiple accept=".png,.jpg,.jpeg,.pdf"></label></p>
        <p><button type="submit">Enregistrer</button></p>
    </form>

    <hr/>

    <h1>List of files</h1>
    <form method="post" action="${pageContext.request.contextPath}/inscription/save_eleve_files" enctype="multipart/form-data">
            <p><label>Nom : <input type="text" name="eleve.nom"></label></p>
            <p><label>Âge : <input type="text" name="eleve.age"></label></p>
            <p><label>Document : <input type="file" name="fichiers" multiple accept=".png,.jpg,.jpeg,.pdf"></label></p>
            <p><button type="submit">Enregistrer</button></p>
        </form>
</body>
</html>