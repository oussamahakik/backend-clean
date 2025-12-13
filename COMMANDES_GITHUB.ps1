# Script PowerShell pour mettre Backend et Frontend sur GitHub
# REMPLACER VOTRE_USERNAME par votre nom d'utilisateur GitHub

$githubUsername = "VOTRE_USERNAME"  # ⚠️ MODIFIER ICI

Write-Host "🚀 Mise en ligne sur GitHub - Backend et Frontend" -ForegroundColor Cyan
Write-Host ""

# ============================================
# BACKEND
# ============================================
Write-Host "📦 BACKEND" -ForegroundColor Yellow
Write-Host ""

$backendPath = "C:\Users\pc\OneDrive - UMONS\Bureau\caisse\caisse"
Set-Location $backendPath

Write-Host "Initialisation Git..." -ForegroundColor Green
if (-not (Test-Path .git)) {
    git init
}

Write-Host "Ajout des fichiers..." -ForegroundColor Green
git add .

Write-Host "Commit initial..." -ForegroundColor Green
git commit -m "Initial commit: Backend Spring Boot - Système de gestion de caisse"

Write-Host "Renommage branche principale..." -ForegroundColor Green
git branch -M main

Write-Host "Ajout du remote GitHub..." -ForegroundColor Green
$backendRemote = "https://github.com/$githubUsername/caisse-backend.git"
git remote remove origin 2>$null
git remote add origin $backendRemote

Write-Host "Push vers GitHub..." -ForegroundColor Green
Write-Host "⚠️ Vous devrez peut-être vous authentifier" -ForegroundColor Yellow
git push -u origin main

Write-Host "✅ Backend poussé avec succès!" -ForegroundColor Green
Write-Host ""

# ============================================
# FRONTEND
# ============================================
Write-Host "🎨 FRONTEND" -ForegroundColor Yellow
Write-Host ""

$frontendPath = "C:\Users\pc\OneDrive - UMONS\Bureau\caisse_manager\caisse-manager-ui"
Set-Location $frontendPath

Write-Host "Ajout des fichiers modifiés..." -ForegroundColor Green
git add .

Write-Host "Commit..." -ForegroundColor Green
git commit -m "Initial commit: Frontend React - Interface de gestion de caisse"

Write-Host "Renommage branche principale..." -ForegroundColor Green
git branch -M main

Write-Host "Ajout du remote GitHub..." -ForegroundColor Green
$frontendRemote = "https://github.com/$githubUsername/caisse-manager-ui.git"
git remote remove origin 2>$null
git remote add origin $frontendRemote

Write-Host "Push vers GitHub..." -ForegroundColor Green
Write-Host "⚠️ Vous devrez peut-être vous authentifier" -ForegroundColor Yellow
git push -u origin main

Write-Host "✅ Frontend poussé avec succès!" -ForegroundColor Green
Write-Host ""

Write-Host "🎉 TOUT EST TERMINÉ!" -ForegroundColor Cyan
Write-Host "Backend: https://github.com/$githubUsername/caisse-backend" -ForegroundColor Blue
Write-Host "Frontend: https://github.com/$githubUsername/caisse-manager-ui" -ForegroundColor Blue

