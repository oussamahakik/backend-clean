# 📋 GUIDE DE TEST DE L'INTÉGRITÉ DE LA BASE DE DONNÉES

## 🎯 Objectif

Vérifier et corriger les problèmes d'intégrité référentielle dans la base de données.

---

## 📝 MÉTHODE 1 : Via l'API REST (Recommandé)

### Prérequis
1. Backend Spring Boot démarré sur `http://localhost:8081`
2. Token JWT d'un utilisateur avec le rôle `SUPER_ADMIN`

### Exécution

#### Option A : Script PowerShell
```powershell
# Depuis PowerShell dans le dossier du projet
.\test-integrity-api.ps1
```

Le script vous demandera le token JWT et exécutera automatiquement :
- ✅ Vérification d'intégrité (`/api/diagnostic/integrity-check`)
- ✅ Récupération des statistiques (`/api/diagnostic/statistics`)

#### Option B : Via cURL
```bash
# Vérification d'intégrité
curl -X GET "http://localhost:8081/api/diagnostic/integrity-check" \
  -H "Authorization: Bearer VOTRE_TOKEN_JWT" \
  -H "Content-Type: application/json"

# Statistiques
curl -X GET "http://localhost:8081/api/diagnostic/statistics" \
  -H "Authorization: Bearer VOTRE_TOKEN_JWT" \
  -H "Content-Type: application/json"
```

#### Option C : Via Postman/Insomnia
1. Créer une requête GET : `http://localhost:8081/api/diagnostic/integrity-check`
2. Ajouter le header : `Authorization: Bearer VOTRE_TOKEN_JWT`
3. Exécuter la requête

---

## 📝 MÉTHODE 2 : Via SQL Direct

### Prérequis
1. Client MySQL/MariaDB installé (ex: MySQL Workbench, DBeaver, ou ligne de commande)
2. Accès à la base de données avec les identifiants

### Connexion
```bash
mysql -h mysql-hakik.alwaysdata.net -u hakik -p hakik_caisse_manager
# Mot de passe: ouss2002
```

### Exécution du script SQL

#### Option A : Ligne de commande
```bash
mysql -h mysql-hakik.alwaysdata.net -u hakik -p hakik_caisse_manager < TEST_INTEGRITE_BDD.sql
```

#### Option B : Dans MySQL Workbench
1. Ouvrir MySQL Workbench
2. Se connecter à la base de données
3. Ouvrir le fichier `TEST_INTEGRITE_BDD.sql`
4. Exécuter le script complet (Ctrl+Shift+Enter)

---

## 🔍 INTERPRÉTATION DES RÉSULTATS

### Vérification d'Intégrité

#### ✅ Aucun problème
Si toutes les requêtes retournent **0 ligne**, cela signifie :
- ✅ Tous les `snack_id` dans `produits` existent dans `snacks`
- ✅ Tous les `snack_id` dans `utilisateurs` existent dans `snacks`
- ✅ Tous les `snack_id` dans `commandes` existent dans `snacks`
- ✅ Tous les `snack_id` dans `ingredients` existent dans `snacks`

#### ⚠️ Problèmes détectés
Si des lignes sont retournées, cela indique des **données orphelines** :
- Des enregistrements qui référencent des `snack_id` qui n'existent plus
- Des incohérences dans la base de données

### Exemple de résultats problématiques
```
table_name    | snack_id_invalide | nombre_occurrences
--------------|-------------------|-------------------
PRODUITS      | 999               | 5
UTILISATEURS  | 999               | 2
```

Cela signifie qu'il y a :
- 5 produits avec `snack_id = 999` qui n'existe pas dans `snacks`
- 2 utilisateurs avec `snack_id = 999` qui n'existe pas dans `snacks`

---

## 🔧 CORRECTION DES PROBLÈMES

### ⚠️ ATTENTION : Toujours faire un backup avant !

```sql
-- Backup de la base avant modification
mysqldump -h mysql-hakik.alwaysdata.net -u hakik -p hakik_caisse_manager > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Options de correction

#### Option 1 : Supprimer les données orphelines (⚠️ DESTRUCTIF)
```sql
-- ⚠️ ATTENTION : Supprime définitivement les données !
DELETE FROM produits WHERE snack_id = 999;
DELETE FROM utilisateurs WHERE snack_id = 999;
```

#### Option 2 : Réassigner à un snack existant (✅ RECOMMANDÉ)
```sql
-- Réassigner à un snack valide (ex: snack_id = 1)
UPDATE produits SET snack_id = 1 WHERE snack_id = 999;
UPDATE utilisateurs SET snack_id = 1 WHERE snack_id = 999;
```

#### Option 3 : Créer le snack manquant (si logique)
```sql
-- Créer le snack s'il devrait exister
INSERT INTO snacks (id, nom, actif) VALUES (999, 'Snack à renommer', true);
```

---

## 📊 STATISTIQUES

Les statistiques vous donnent un aperçu de la base :
- Nombre de produits par snack
- Nombre d'utilisateurs par snack
- Nombre de commandes par snack

Cela permet de détecter :
- Des snacks vides (0 produits, 0 utilisateurs)
- Des incohérences dans les comptages

---

## 🐛 DÉPANNAGE

### Erreur : "Access Denied"
- Vérifier que vous utilisez un token SUPER_ADMIN
- Vérifier que le backend est démarré

### Erreur : "Connection refused"
- Vérifier que le backend Spring Boot est démarré sur le port 8081
- Vérifier la configuration dans `application.properties`

### Erreur SQL : "Table doesn't exist"
- Vérifier que les noms de tables correspondent (snacks, produits, etc.)
- Vérifier que vous êtes connecté à la bonne base de données

### Erreur : "Token expired"
- Se reconnecter pour obtenir un nouveau token
- Vérifier la validité du token JWT

---

## ✅ CHECKLIST DE VÉRIFICATION

- [ ] Backend Spring Boot démarré
- [ ] Token JWT SUPER_ADMIN obtenu
- [ ] Script de test exécuté
- [ ] Résultats analysés
- [ ] Backup créé (si corrections nécessaires)
- [ ] Corrections appliquées (si nécessaire)
- [ ] Nouvelle vérification effectuée pour confirmer

---

## 📞 SUPPORT

En cas de problème :
1. Vérifier les logs du backend (`application.log`)
2. Vérifier les logs de la base de données
3. Contacter l'administrateur système

---

**Date de création** : 2024  
**Version** : 1.0













