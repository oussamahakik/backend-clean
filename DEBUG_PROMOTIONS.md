# 🔍 Guide de Debug : Pourquoi les promotions ne s'appliquent pas

## ✅ Modifications apportées

J'ai ajouté des **logs de debug** détaillés dans la console du navigateur pour identifier le problème.

## 📋 Étapes de vérification

### 1. Ouvrir la Console du Navigateur
- Appuyez sur `F12` ou `Ctrl+Shift+I`
- Allez dans l'onglet **Console**

### 2. Vérifier le chargement des promotions

Lors du chargement de l'application, vous devriez voir :
```
✅ Promotions chargées: X [Array of promotions]
```

**Si cette ligne n'apparaît pas ou montre `0` promotions :**
- Vérifiez que vous êtes connecté (token valide)
- Vérifiez que le `snackId` est correct
- Vérifiez l'endpoint `/api/promotions/actives` dans l'onglet **Network**

### 3. Vérifier les promotions dans le panier

Quand vous ajoutez un produit au panier, vous devriez voir :
```
🔍 Promotions disponibles: [Array]
🔍 Panier: [Array]
🔍 Item: NomProduit, Catégorie: CATEGORIE, Prix: 10.00
🔍 Recherche promo pour produit: {id: 1, nom: "...", categorie: "...", prixInitial: 10}
🔍 Promotions à vérifier: [Array]
```

### 4. Vérifier le matching des catégories

Si une promotion existe pour la catégorie, vous verrez :
```
🔍 Promotion catégorie vérifiée: {promoNom: "...", promoCategorie: "TACOS", produitCategorie: "Tacos", matchCategorie: true/false, ...}
```

**Points à vérifier :**
- ✅ `matchCategorie` doit être `true`
- ✅ `actif` doit être `true`
- ✅ `isActive` doit être `true`

### 5. Problèmes courants

#### Problème 1 : Promotions non chargées
```
✅ Promotions chargées: 0 []
```
**Solution :**
- Vérifier l'endpoint backend `/api/promotions/actives`
- Vérifier le header `X-Snack-ID`

#### Problème 2 : Catégorie ne correspond pas
```
🔍 Promotion catégorie vérifiée: {matchCategorie: false, ...}
```
**Solution :**
- Vérifier que la catégorie du produit correspond EXACTEMENT à celle de la promotion
- La comparaison est **insensible à la casse** (TACOS = Tacos = tacos)
- Vérifier qu'il n'y a pas d'espaces en début/fin

#### Problème 3 : Promotion expirée
```
❌ Promotion expirée ou pas encore active: {nom: "...", ...}
```
**Solution :**
- Vérifier les dates `dateDebut` et `dateFin` de la promotion
- S'assurer que la date actuelle est entre ces deux dates

#### Problème 4 : Produit sans catégorie
```
❌ Aucune promotion catégorie trouvée pour "undefined"
```
**Solution :**
- Vérifier que le produit a bien une propriété `categorie`
- Vérifier que cette catégorie est remplie en base de données

### 6. Test rapide

1. Créez une promotion simple :
   - Nom : "Test -30%"
   - Type : POURCENTAGE
   - Valeur : 30
   - Catégorie : "Tacos" (ou la catégorie d'un de vos produits)
   - Date début : Aujourd'hui
   - Date fin : Dans 30 jours
   - Actif : ✅

2. Rechargez l'application

3. Ouvrez la console (F12)

4. Ajoutez un produit de catégorie "Tacos" au panier

5. Regardez les logs dans la console

6. Si vous voyez `✅ Promotion trouvée`, la promotion devrait s'afficher

---

## 🐛 Si les logs ne montrent rien

Si vous ne voyez aucun log dans la console, cela signifie que :
- Soit les promotions ne sont pas chargées (`promotions` est vide)
- Soit la fonction `findBestPromotionForProduct` n'est pas appelée

Dans ce cas, vérifiez :
1. Que `promotions` est passé en prop à `OrderTicket` et `ProductList`
2. Que `cartWithPromotions` est bien calculé dans `OrderTicket`

---

## 📝 Envoyer les logs

Si les promotions ne fonctionnent toujours pas, copiez-collez les logs de la console dans votre réponse pour que je puisse identifier le problème exact.

