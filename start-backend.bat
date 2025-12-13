@echo off
title Caisse Manager - Backend Server
color 0A

echo ===================================================
echo   DEMARRAGE DU SERVEUR BACKEND (Spring Boot)
echo ===================================================
echo.

:: Vérifier si on est dans le bon répertoire
if not exist "gradlew.bat" (
    echo ERREUR: gradlew.bat introuvable. Assurez-vous d'etre dans le repertoire caisse/caisse
    pause
    exit /b 1
)

echo [INFO] Compilation et demarrage du serveur...
echo [INFO] Port: 8081
echo [INFO] Base de donnees: MariaDB (AlwaysData)
echo.

:: Lancer le serveur
call gradlew.bat bootRun

pause

