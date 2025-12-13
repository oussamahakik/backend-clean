# 📦 Guide pour mettre Backend et Frontend sur GitHub

## 🔧 Prérequis

1. Avoir un compte GitHub
2. Créer 2 nouveaux dépôts sur GitHub :
   - `caisse-backend` (ou le nom que vous voulez)
   - `caisse-manager-ui` (ou le nom que vous voulez)

⚠️ **Ne pas initialiser les dépôts avec README.md** lors de la création sur GitHub.

---

## 🔷 BACKEND (Java Spring Boot)

### 1. Aller dans le dossier backend
```powershell
cd "C:\Users\pc\OneDrive - UMONS\Bureau\caisse\caisse"
```

### 2. Initialiser Git (si pas déjà fait)
```powershell
git init
```

### 3. Vérifier le .gitignore
Le fichier `.gitignore` existe déjà et ignore les fichiers de build. Si besoin, vérifier qu'il contient :
```
.gradle
build/
.idea
*.iml
bin/
```

### 4. Ajouter tous les fichiers
```powershell
git add .
```

### 5. Faire le commit initial
```powershell
git commit -m "Initial commit: Backend Spring Boot - Système de gestion de caisse"
```

### 6. Renommer la branche principale (optionnel, mais recommandé)
```powershell
git branch -M main
```

### 7. Ajouter le remote GitHub
**Remplacez `VOTRE_USERNAME` par votre nom d'utilisateur GitHub**
```powershell
git remote add origin https://github.com/VOTRE_USERNAME/caisse-backend.git
```

### 8. Pousser vers GitHub
```powershell
git push -u origin main
```

---

## 🎨 FRONTEND (React)

### 1. Aller dans le dossier frontend
```powershell
cd "C:\Users\pc\OneDrive - UMONS\Bureau\caisse_manager\caisse-manager-ui"
```

### 2. Vérifier l'état Git
```powershell
git status
```

### 3. Ajouter tous les fichiers modifiés
```powershell
git add .
```

### 4. Faire le commit
```powershell
git commit -m "Initial commit: Frontend React - Interface de gestion de caisse"
```

### 5. Vérifier le remote actuel
```powershell
git remote -v
```

### 6. Si un remote existe déjà, le changer
```powershell
git remote set-url origin https://github.com/VOTRE_USERNAME/caisse-manager-ui.git
```

### 7. Si aucun remote n'existe, l'ajouter
```powershell
git remote add origin https://github.com/VOTRE_USERNAME/caisse-manager-ui.git
```

### 8. Pousser vers GitHub
```powershell
git branch -M main
git push -u origin main
```

---

## 📋 Commandes complètes (copier-coller)

### Backend
```powershell
# Se placer dans le dossier backend
cd "C:\Users\pc\OneDrive - UMONS\Bureau\caisse\caisse"

# Initialiser Git
git init

# Ajouter les fichiers
git add .

# Commit initial
git commit -m "Initial commit: Backend Spring Boot - Système de gestion de caisse"

# Renommer branche principale
git branch -M main

# Ajouter remote (REMPLACER VOTRE_USERNAME)
git remote add origin https://github.com/VOTRE_USERNAME/caisse-backend.git

# Pousser vers GitHub
git push -u origin main
```

### Frontend
```powershell
# Se placer dans le dossier frontend
cd "C:\Users\pc\OneDrive - UMONS\Bureau\caisse_manager\caisse-manager-ui"

# Ajouter les fichiers modifiés
git add .

# Commit
git commit -m "Initial commit: Frontend React - Interface de gestion de caisse"

# Vérifier/changer le remote (REMPLACER VOTRE_USERNAME)
git remote set-url origin https://github.com/VOTRE_USERNAME/caisse-manager-ui.git
# OU si pas de remote :
# git remote add origin https://github.com/VOTRE_USERNAME/caisse-manager-ui.git

# Renommer branche principale
git branch -M main

# Pousser vers GitHub
git push -u origin main
```

---

## 🔐 Authentification GitHub

Si GitHub vous demande une authentification, vous pouvez :

### Option 1 : Token d'accès personnel (recommandé)
1. Aller sur GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Générer un nouveau token avec les permissions `repo`
3. Utiliser ce token comme mot de passe lors du push

### Option 2 : GitHub CLI
```powershell
# Installer GitHub CLI si pas déjà fait
winget install --id GitHub.cli

# Se connecter
gh auth login

# Ensuite les push fonctionneront sans authentification
```

### Option 3 : SSH (recommandé pour utilisation longue)
1. Générer une clé SSH
2. L'ajouter sur GitHub → Settings → SSH and GPG keys
3. Utiliser l'URL SSH : `git@github.com:VOTRE_USERNAME/caisse-backend.git`

---

## ✅ Vérification

Après le push, vérifier sur GitHub que :
- ✅ Tous les fichiers sont présents
- ✅ Le README.md est visible
- ✅ Les fichiers sensibles (application.properties avec mots de passe) sont exclus

---

## 🛡️ Sécurité - Fichiers à exclure

### Backend - Vérifier que `.gitignore` contient :
- `application.properties` (si contient des secrets)
- Créer `application.properties.example` avec des valeurs de démo
- `.gradle/`
- `build/`
- `*.log`

### Frontend - Vérifier que `.gitignore` contient :
- `node_modules/`
- `build/`
- `.env` (si contient des secrets)
- Créer `.env.example` avec des variables de démo

---

## 📝 README.md recommandé

Créer un README.md pour chaque projet :

### Backend README.md
```markdown
# Caisse Manager - Backend

Système de gestion de caisse pour restaurants et snacks.

## Technologies
- Java 21
- Spring Boot 3.5.7
- Spring Security
- JWT
- Hibernate/JPA
- MariaDB

## Configuration
Copier `application.properties.example` vers `application.properties` et configurer.
```

### Frontend README.md
```markdown
# Caisse Manager - Frontend

Interface React pour la gestion de caisse.

## Technologies
- React
- TailwindCSS
- Axios

## Installation
npm install

## Lancement
npm start
```

---

## 🚀 Mises à jour futures

Pour pousser des modifications :

```powershell
# Backend ou Frontend
git add .
git commit -m "Description des modifications"
git push
```

---

**Note :** Remplacez `VOTRE_USERNAME` par votre nom d'utilisateur GitHub dans toutes les commandes.

