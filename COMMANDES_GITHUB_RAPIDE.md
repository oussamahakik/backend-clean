# 🚀 Commandes rapides pour GitHub

## 📋 Étapes rapides

### 1. Créer les dépôts sur GitHub
- Aller sur https://github.com/new
- Créer `caisse-backend` (sans README)
- Créer `caisse-manager-ui` (sans README)

### 2. BACKEND - Commandes complètes

```powershell
cd "C:\Users\pc\OneDrive - UMONS\Bureau\caisse\caisse"
git init
git add .
git commit -m "Initial commit: Backend Spring Boot"
git branch -M main
git remote add origin https://github.com/VOTRE_USERNAME/caisse-backend.git
git push -u origin main
```

### 3. FRONTEND - Commandes complètes

```powershell
cd "C:\Users\pc\OneDrive - UMONS\Bureau\caisse_manager\caisse-manager-ui"
git add .
git commit -m "Initial commit: Frontend React"
git branch -M main
git remote set-url origin https://github.com/VOTRE_USERNAME/caisse-manager-ui.git
git push -u origin main
```

---

## ⚡ OU utiliser le script PowerShell

1. Ouvrir `COMMANDES_GITHUB.ps1`
2. Modifier `$githubUsername = "VOTRE_USERNAME"`
3. Exécuter : `.\COMMANDES_GITHUB.ps1`

---

## 🔐 Authentification

Si GitHub demande un mot de passe, utiliser un **Personal Access Token** :
1. GitHub → Settings → Developer settings → Personal access tokens → Generate new token
2. Cocher `repo`
3. Utiliser le token comme mot de passe

---

**⚠️ REMPLACER `VOTRE_USERNAME` par votre nom d'utilisateur GitHub**

