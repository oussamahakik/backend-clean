# 🎁 Documentation : Application Automatique des Promotions

## 📋 Vue d'ensemble

Le système applique **automatiquement** les promotions actives aux produits lors de la création d'une commande. Les promotions peuvent être appliquées :

1. **Par catégorie** : Tous les produits d'une catégorie spécifique (ex: "Tacos", "Burgers")
2. **Par produit** : Sur un produit spécifique (via `produitId`)

---

## 🔧 Fonctionnement

### 1. Création de Promotion

Lorsqu'une promotion est créée avec un champ `categorie`, elle s'applique automatiquement à tous les produits de cette catégorie lors de la création de commande.

**Exemple :**
```json
{
  "nom": "Promo Tacos -10%",
  "typePromotion": "POURCENTAGE",
  "valeur": 10.0,
  "categorie": "Tacos",
  "dateDebut": "2024-12-01",
  "dateFin": "2024-12-31",
  "actif": true
}
```

Cette promotion sera automatiquement appliquée à tous les produits de la catégorie "Tacos".

### 2. Application Automatique lors de la Création de Commande

Quand une commande est créée (`POST /api/commandes`), le système :

1. **Pour chaque produit** de la commande :
   - Récupère le prix initial (depuis le Frontend ou le prix de base du produit)
   - Vérifie s'il existe une promotion active pour :
     - Le produit spécifique (priorité 1)
     - La catégorie du produit (priorité 2)
   - Applique la promotion automatiquement
   - Sauvegarde le prix réduit dans `LigneCommande.prixUnitaire`

2. **Calcule le total** de la commande avec les prix réduits

---

## 📊 Types de Promotions

### POURCENTAGE
Réduction en pourcentage sur le prix.

**Exemple :** 
- Prix initial : 10.0€
- Promotion : 10% de réduction
- Prix final : 9.0€ (10.0 - 10%)

### MONTANT_FIXE
Réduction d'un montant fixe.

**Exemple :**
- Prix initial : 10.0€
- Promotion : -2€
- Prix final : 8.0€ (10.0 - 2.0)

### CODE_PROMO
Réduction via code promo (traitée comme pourcentage pour l'instant).

---

## 🔍 Priorité d'Application

Si plusieurs promotions existent pour un même produit :

1. **Priorité 1** : Promotion sur le produit spécifique (via `produitId`)
2. **Priorité 2** : Promotion sur la catégorie (via `categorie`)

Si plusieurs promotions existent au même niveau, la première trouvée est appliquée.

---

## 💻 Code

### Service : `PromotionService`

Le service `PromotionService` contient les méthodes :

- `trouverPromotionActivePourProduit(Produit, Long snackId)` : Trouve une promotion pour la catégorie
- `trouverPromotionActivePourProduitId(Long produitId, Long snackId)` : Trouve une promotion pour un produit
- `calculerPrixAvecPromotion(double prixInitial, Promotion)` : Calcule le prix réduit
- `appliquerPromotionAutomatique(Produit, Long snackId, double prixInitial)` : Applique automatiquement la meilleure promotion

### Controller : `CommandeController`

Dans la méthode `creerCommande()`, avant de sauvegarder le prix :

```java
// 1. Prix de base (depuis Frontend ou prix produit)
double prixDeBase = (ligneReq.getPrixFinal() != null) 
    ? ligneReq.getPrixFinal() 
    : produit.getPrix();

// 2. Appliquer automatiquement la promotion
double prixAvecPromotion = promotionService
    .appliquerPromotionAutomatique(produit, snackId, prixDeBase);

// 3. Sauvegarder le prix réduit
ligne.setPrixUnitaire(prixAvecPromotion);
```

---

## ✅ Exemple d'Utilisation

### Scénario : Promo -10% sur tous les Tacos

1. **Créer une promotion :**
```bash
POST /api/promotions
{
  "nom": "Promo Tacos -10%",
  "typePromotion": "POURCENTAGE",
  "valeur": 10.0,
  "categorie": "Tacos",
  "dateDebut": "2024-12-01",
  "dateFin": "2024-12-31",
  "actif": true,
  "snackId": 1
}
```

2. **Créer une commande avec un Tacos :**
```bash
POST /api/commandes
{
  "typePaiement": "ESPECES",
  "articles": [
    {
      "produitId": 5,
      "quantite": 1,
      "details": "",
      "prixFinal": 10.0
    }
  ],
  "remise": 0.0
}
```

3. **Résultat :**
- Le produit "Tacos XL" (catégorie "Tacos") a un prix initial de 10.0€
- La promotion -10% est automatiquement appliquée
- Le prix sauvegardé en BDD : **9.0€**
- Le total de la commande : **9.0€**

---

## 🧪 Tests

Le fichier `TestPromotionAutomatique.java` contient des tests qui vérifient :

1. ✅ Application automatique d'une promotion POURCENTAGE sur une catégorie
2. ✅ Application automatique d'une promotion MONTANT_FIXE
3. ✅ Vérification en BDD que le prix réduit est bien sauvegardé

---

## 📝 Notes Importantes

1. **Les promotions sont appliquées automatiquement** - Aucune action manuelle nécessaire
2. **Priorité produit > catégorie** - Si une promotion existe sur un produit ET sur sa catégorie, celle sur le produit est appliquée
3. **Date de validité** - Seules les promotions actives et dans leur période de validité sont appliquées
4. **snackId** - Les promotions sont filtrées par snack (multi-tenant)
5. **Prix avec suppléments** - Si le Frontend envoie un `prixFinal` (avec suppléments), la promotion s'applique sur ce prix total

---

## 🔐 Sécurité

- Les promotions sont filtrées par `snackId` pour éviter les fuites entre restaurants
- Seules les promotions actives (`actif=true`) sont appliquées
- Les dates de début/fin sont vérifiées avant application

