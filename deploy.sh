#!/usr/bin/env bash
# deploy.sh : compile le Framework, crée le jar, assemble le webapp Test
# et le déploie dans Tomcat. À lancer depuis Git Bash (ou WSL).
set -euo pipefail

# ---------------------------------------------------------------
# Séparateur de classpath + conversion des chemins
#   Windows (Git Bash) : ";" et chemins natifs "C:/..." (cygpath -m)
#   Linux / WSL        : ":" et chemins inchangés
# Pourquoi : javac est un programme Windows natif. Git Bash convertit les
# arguments de la ligne de commande, mais PAS le contenu des fichiers @argfile.
# Un chemin "/c/Users/..." y serait lu comme "\c\Users\..." -> fichier introuvable.
# ---------------------------------------------------------------
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
        SEP=";"
        to_native() { cygpath -m "$1"; }
        ;;
    *)
        SEP=":"
        to_native() { printf '%s' "$1"; }
        ;;
esac

# ---------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------
# TOMCAT_HOME doit être défini (ex : export TOMCAT_HOME="C:/apache-tomcat-10.1.x")
if [ -z "${TOMCAT_HOME:-}" ]; then
    echo "ERREUR : la variable TOMCAT_HOME n'est pas définie." >&2
    echo "  Exemple : export TOMCAT_HOME=\"C:/apache-tomcat-10.1.x\"" >&2
    exit 1
fi
TOMCAT_HOME="$(to_native "$TOMCAT_HOME")"

APP_NAME="Test"
ROOT_DIR="$(to_native "$(cd "$(dirname "$0")" && pwd)")"

FRAMEWORK_SRC="$ROOT_DIR/Framework/src/main/java"
TEST_SRC="$ROOT_DIR/Test/src/main/java"
TEST_WEBAPP="$ROOT_DIR/Test/src/main/webapp"

BUILD_DIR="$ROOT_DIR/build"
FRAMEWORK_CLASSES="$BUILD_DIR/framework/classes"
JAR_FILE="$BUILD_DIR/miniframework.jar"
WEBAPP_BUILD="$BUILD_DIR/$APP_NAME"

SERVLET_API="$TOMCAT_HOME/lib/servlet-api.jar"

# ---------------------------------------------------------------
# Outils JDK : on préfère JAVA_HOME s'il est défini, sinon le PATH
# ---------------------------------------------------------------
if [ -n "${JAVA_HOME:-}" ] && { [ -x "$JAVA_HOME/bin/javac" ] || [ -x "$JAVA_HOME/bin/javac.exe" ]; }; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAR="$JAVA_HOME/bin/jar"
else
    JAVAC="javac"
    JAR="jar"
fi

command -v "$JAVAC" >/dev/null 2>&1 || { echo "ERREUR : javac introuvable (définis JAVA_HOME vers un JDK 17)." >&2; exit 1; }
command -v "$JAR"   >/dev/null 2>&1 || { echo "ERREUR : jar introuvable (définis JAVA_HOME vers un JDK 17, pas un JRE)." >&2; exit 1; }

# ---------------------------------------------------------------
# Vérifications préalables
# ---------------------------------------------------------------
[ -f "$SERVLET_API" ] || { echo "ERREUR : $SERVLET_API introuvable. Vérifie TOMCAT_HOME." >&2; exit 1; }
[ -d "$TOMCAT_HOME/webapps" ] || { echo "ERREUR : $TOMCAT_HOME/webapps introuvable." >&2; exit 1; }

# Compile tous les .java d'un dossier source vers un dossier de sortie.
# Ne fait rien (avec un avertissement) s'il n'y a aucun fichier source.
# Usage : compile_sources <src_dir> <out_dir> <classpath> <label>
compile_sources() {
    local src_dir="$1" out_dir="$2" cp="$3" label="$4"
    local list="$BUILD_DIR/${label}-sources.txt"

    mkdir -p "$out_dir"
    if [ -d "$src_dir" ]; then
        # Chaque chemin est entouré de guillemets : dans un @argfile, javac
        # interprète les espaces et les antislashs.
        find "$src_dir" -name "*.java" | sed 's/.*/"&"/' > "$list"
    else
        : > "$list"
    fi

    if [ -s "$list" ]; then
        echo "   -> $(wc -l < "$list") fichier(s) .java"
        "$JAVAC" -encoding UTF-8 -parameters --release 17 \
                 -cp "$cp" -d "$out_dir" "@$list"
    else
        echo "   -> (aucun fichier .java pour l'instant, compilation ignorée)"
    fi
}

# ---------------------------------------------------------------
# 1. Nettoyage de build/
# ---------------------------------------------------------------
echo "[1/5] Nettoyage de build/"
rm -rf "$BUILD_DIR"
mkdir -p "$FRAMEWORK_CLASSES"

# ---------------------------------------------------------------
# 2. Compilation du Framework (servlet-api.jar en classpath, non embarqué)
# ---------------------------------------------------------------
echo "[2/5] Compilation du Framework"
compile_sources "$FRAMEWORK_SRC" "$FRAMEWORK_CLASSES" "$SERVLET_API" "framework"

# ---------------------------------------------------------------
# 3. Packaging du jar
# ---------------------------------------------------------------
echo "[3/5] Création de miniframework.jar"
"$JAR" --create --file "$JAR_FILE" -C "$FRAMEWORK_CLASSES" .

# ---------------------------------------------------------------
# 4. Assemblage du webapp Test + compilation de ses sources
# ---------------------------------------------------------------
echo "[4/5] Assemblage du webapp $APP_NAME"
mkdir -p "$WEBAPP_BUILD"
[ -d "$TEST_WEBAPP" ] || { echo "ERREUR : $TEST_WEBAPP introuvable." >&2; exit 1; }
cp -r "$TEST_WEBAPP"/. "$WEBAPP_BUILD"/

mkdir -p "$WEBAPP_BUILD/WEB-INF/lib" "$WEBAPP_BUILD/WEB-INF/classes"
cp "$JAR_FILE" "$WEBAPP_BUILD/WEB-INF/lib/"

echo "   Compilation des sources $APP_NAME"
compile_sources "$TEST_SRC" "$WEBAPP_BUILD/WEB-INF/classes" \
                "${SERVLET_API}${SEP}${JAR_FILE}" "test"

# ---------------------------------------------------------------
# 5. Déploiement dans Tomcat
# ---------------------------------------------------------------
echo "[5/5] Déploiement dans $TOMCAT_HOME/webapps/$APP_NAME"
rm -rf "$TOMCAT_HOME/webapps/$APP_NAME"
cp -r "$WEBAPP_BUILD" "$TOMCAT_HOME/webapps/$APP_NAME"

echo ""
echo "Déploiement terminé -> http://localhost:8080/$APP_NAME/"
