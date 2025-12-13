# Script PowerShell pour tester les vérifications d'intégrité via l'API
# Utilise le DiagnosticController créé dans le backend

$apiBaseUrl = "http://localhost:8081"
$token = Read-Host "Entrez le token JWT (laissez vide si pas encore connecté)"

# Fonction pour tester l'endpoint d'intégrité
function Test-IntegrityCheck {
    Write-Host "`n=== VÉRIFICATION DE L'INTÉGRITÉ DE LA BASE DE DONNÉES ===" -ForegroundColor Cyan
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$apiBaseUrl/api/diagnostic/integrity-check" `
            -Method GET `
            -Headers $headers `
            -ErrorAction Stop
        
        Write-Host "`n✅ Vérification terminée avec succès!" -ForegroundColor Green
        Write-Host "`n📊 RÉSUMÉ:" -ForegroundColor Yellow
        Write-Host "Total de problèmes détectés: $($response.totalIssues)" -ForegroundColor $(if ($response.hasIssues) { "Red" } else { "Green" })
        Write-Host "A des problèmes: $($response.hasIssues)" -ForegroundColor $(if ($response.hasIssues) { "Red" } else { "Green" })
        
        if ($response.summary -and $response.summary.Count -gt 0) {
            Write-Host "`n📋 DÉTAILS PAR TYPE:" -ForegroundColor Yellow
            $response.summary.PSObject.Properties | ForEach-Object {
                Write-Host "  - $($_.Name): $($_.Value)" -ForegroundColor $(if ($_.Value -gt 0) { "Red" } else { "Green" })
            }
        }
        
        if ($response.issues -and $response.issues.Count -gt 0) {
            Write-Host "`n⚠️ PROBLÈMES DÉTECTÉS:" -ForegroundColor Red
            $response.issues | ForEach-Object {
                Write-Host "  - Table: $($_.tableName), ID invalide: $($_.invalidId), Description: $($_.description)" -ForegroundColor Red
            }
        } else {
            Write-Host "`n✅ Aucun problème d'intégrité détecté!" -ForegroundColor Green
        }
        
        return $response
    }
    catch {
        Write-Host "`n❌ ERREUR lors de la vérification:" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Réponse du serveur: $responseBody" -ForegroundColor Yellow
        }
        
        return $null
    }
}

# Fonction pour tester l'endpoint de statistiques
function Test-Statistics {
    Write-Host "`n=== STATISTIQUES PAR SNACK ===" -ForegroundColor Cyan
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$apiBaseUrl/api/diagnostic/statistics" `
            -Method GET `
            -Headers $headers `
            -ErrorAction Stop
        
        Write-Host "`n✅ Statistiques récupérées avec succès!" -ForegroundColor Green
        Write-Host "`n📊 STATISTIQUES PAR SNACK:" -ForegroundColor Yellow
        
        $response | ForEach-Object {
            Write-Host "`n  Snack ID: $($_.snackId)" -ForegroundColor Cyan
            Write-Host "    Nom: $($_.snackNom)" -ForegroundColor White
            Write-Host "    Produits: $($_.nombreProduits)" -ForegroundColor Green
            Write-Host "    Utilisateurs: $($_.nombreUtilisateurs)" -ForegroundColor Green
            Write-Host "    Commandes: $($_.nombreCommandes)" -ForegroundColor Green
        }
        
        return $response
    }
    catch {
        Write-Host "`n❌ ERREUR lors de la récupération des statistiques:" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        return $null
    }
}

# Exécution des tests
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST DES VÉRIFICATIONS D'INTÉGRITÉ" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test de vérification d'intégrité
$integrityResult = Test-IntegrityCheck

# Test des statistiques
$statistics = Test-Statistics

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  TESTS TERMINÉS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan













