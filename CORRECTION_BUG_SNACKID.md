# 🔧 CORRECTION DU BUG : snackId manquant lors de la création de produit

## 🐛 Problème identifié

**Erreur :** `Field 'snackId' doesn't have a default value`

**Symptôme :** Lors de la création d'un produit via `POST /api/produits`, le champ `snack_id` était `null` dans la base de données, causant une erreur SQL.

**Cause :** Le `Produit` reçu dans le body JSON pouvait avoir un `snackId` null ou absent, et même après `produit.setSnackId(snackId)`, la valeur pouvait être écrasée lors de la désérialisation Jackson.

---

## ✅ Solution appliquée

**Fichier modifié :** `ProduitController.java` - Méthode `ajouterProduit()`

### Avant (code problématique) :
```java
@PostMapping
public ResponseEntity<?> ajouterProduit(
        @RequestBody Produit produit, 
        @RequestHeader("X-Snack-ID") Long snackId) {
    
    produit.setSnackId(snackId); // ⚠️ Pouvait être écrasé par la désérialisation
    // ...
}
```

### Après (code corrigé) :
```java
@PostMapping
public ResponseEntity<?> ajouterProduit(
        @RequestBody Produit produitRequest, 
        @RequestHeader("X-Snack-ID") Long snackId) {
    
    // ✅ Créer un nouveau Produit pour éviter tout problème avec la désérialisation
    Produit produit = new Produit();
    produit.setNom(produitRequest.getNom());
    produit.setPrix(produitRequest.getPrix());
    produit.setCategorie(produitRequest.getCategorie());
    produit.setImage(produitRequest.getImage());
    produit.setSnackId(snackId); // ✅ FORCER le snackId depuis le header (sécurité)
    produit.setDisponible(produitRequest.getDisponible() != null ? produitRequest.getDisponible() : true);
    
    // ✅ Vérification finale
    if (produit.getSnackId() == null) {
        log.error("Erreur critique : snackId est null avant la sauvegarde !");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Erreur interne : snackId non défini");
    }
    
    // ...
}
```

### Points clés de la correction :

1. **Création d'un nouveau `Produit`** au lieu de modifier celui du body
2. **Copie explicite des champs** depuis `produitRequest`
3. **Forçage du `snackId` depuis le header** (sécurité)
4. **Vérification finale** avant la sauvegarde
5. **Logs améliorés** pour le debugging

---

## 🧪 Test de validation

**Fichier de test :** `TestCreationProduit.java`

Le test vérifie :
- ✅ Création d'un snack
- ✅ Authentification du manager
- ✅ Création de **3 produits** (simulation d'un menu complet)
- ✅ Vérification BDD que chaque produit a le `snackId` correctement sauvegardé
- ✅ Vérification que tous les produits ont le même `snackId`

**Résultat :** ✅ **TEST RÉUSSI** - Tous les produits sont créés avec le `snackId` correctement sauvegardé.

---

## 📋 Vérification en production

Pour vérifier que le correctif fonctionne :

1. **Créer un produit via le Frontend :**
   - Aller dans "Menu" → "Ajouter un Produit"
   - Remplir le formulaire (nom, prix, catégorie)
   - Cliquer sur "Ajouter au Menu"

2. **Vérifier dans la base de données :**
   ```sql
   SELECT id, nom, prix, snack_id FROM produits ORDER BY id DESC LIMIT 1;
   ```
   Le champ `snack_id` doit être rempli (pas NULL).

3. **Vérifier dans les logs :**
   ```
   Création du produit 'Nom du produit' pour le snackId: X
   Produit créé avec succès, ID: Y, snackId: X
   ```

---

## ✅ Statut

**Correction appliquée :** ✅  
**Test de validation :** ✅ PASSÉ  
**Prêt pour la production :** ✅

Le bug est corrigé et testé. La création de produits fonctionne maintenant correctement avec le `snackId` toujours défini.

