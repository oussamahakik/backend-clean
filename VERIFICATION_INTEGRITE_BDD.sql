-- ============================================================
-- SCRIPT DE VÉRIFICATION DE L'INTÉGRITÉ RÉFÉRENTIELLE
-- ============================================================
-- Ce script vérifie que toutes les clés étrangères sont valides
-- et que les relations entre les tables sont cohérentes.

-- 1. Vérifier que les snack_id dans la table produits existent dans snacks
-- ========================================================================
SELECT 
    'PRODUITS' AS table_name,
    p.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM produits p
LEFT JOIN snacks s ON p.snack_id = s.id
WHERE s.id IS NULL
GROUP BY p.snack_id;

-- Si cette requête retourne des lignes, il y a des produits avec des snack_id invalides.

-- 2. Vérifier que les snack_id dans la table utilisateurs existent dans snacks
-- ==============================================================================
SELECT 
    'UTILISATEURS' AS table_name,
    u.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM utilisateurs u
LEFT JOIN snacks s ON u.snack_id = s.id
WHERE u.snack_id IS NOT NULL AND s.id IS NULL
GROUP BY u.snack_id;

-- Si cette requête retourne des lignes, il y a des utilisateurs avec des snack_id invalides.

-- 3. Vérifier que les snack_id dans la table commandes existent dans snacks
-- ===========================================================================
SELECT 
    'COMMANDES' AS table_name,
    c.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM commandes c
LEFT JOIN snacks s ON c.snack_id = s.id
WHERE s.id IS NULL
GROUP BY c.snack_id;

-- 4. Vérifier que les snack_id dans la table ingredients existent dans snacks
-- =============================================================================
SELECT 
    'INGREDIENTS' AS table_name,
    i.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM ingredients i
LEFT JOIN snacks s ON i.snack_id = s.id
WHERE s.id IS NULL
GROUP BY i.snack_id;

-- 5. Vérifier que les produits_id dans ligne_commande existent dans produits
-- ============================================================================
SELECT 
    'LIGNE_COMMANDE' AS table_name,
    lc.produit_id AS produit_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM ligne_commande lc
LEFT JOIN produits p ON lc.produit_id = p.id
WHERE p.id IS NULL
GROUP BY lc.produit_id;

-- 6. Vérifier que les commande_id dans ligne_commande existent dans commandes
-- ============================================================================
SELECT 
    'LIGNE_COMMANDE' AS table_name,
    lc.commande_id AS commande_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM ligne_commande lc
LEFT JOIN commandes c ON lc.commande_id = c.id
WHERE c.id IS NULL
GROUP BY lc.commande_id;

-- 7. RÉSUMÉ GÉNÉRAL : Comptage des incohérences
-- ===============================================
SELECT 
    'RÉSUMÉ' AS type_verification,
    'Produits orphelins' AS description,
    COUNT(*) AS nombre_incoherences
FROM produits p
LEFT JOIN snacks s ON p.snack_id = s.id
WHERE s.id IS NULL

UNION ALL

SELECT 
    'RÉSUMÉ',
    'Utilisateurs orphelins',
    COUNT(*)
FROM utilisateurs u
LEFT JOIN snacks s ON u.snack_id = s.id
WHERE u.snack_id IS NOT NULL AND s.id IS NULL

UNION ALL

SELECT 
    'RÉSUMÉ',
    'Commandes orphelines',
    COUNT(*)
FROM commandes c
LEFT JOIN snacks s ON c.snack_id = s.id
WHERE s.id IS NULL

UNION ALL

SELECT 
    'RÉSUMÉ',
    'Ingrédients orphelins',
    COUNT(*)
FROM ingredients i
LEFT JOIN snacks s ON i.snack_id = s.id
WHERE s.id IS NULL;

-- ============================================================
-- REQUÊTES DE NETTOYAGE (À UTILISER AVEC PRÉCAUTION !)
-- ============================================================
-- ATTENTION : Ne pas exécuter ces requêtes en production sans backup !

-- Supprimer les produits orphelins (non recommandé, préférez la correction manuelle)
-- DELETE FROM produits WHERE snack_id NOT IN (SELECT id FROM snacks);

-- Supprimer les utilisateurs orphelins (non recommandé, préférez la correction manuelle)
-- DELETE FROM utilisateurs WHERE snack_id IS NOT NULL AND snack_id NOT IN (SELECT id FROM snacks);

-- ============================================================
-- REQUÊTES DE STATISTIQUES
-- ============================================================

-- Nombre de produits par snack
SELECT 
    s.id AS snack_id,
    s.nom AS snack_nom,
    COUNT(p.id) AS nombre_produits
FROM snacks s
LEFT JOIN produits p ON s.id = p.snack_id
GROUP BY s.id, s.nom
ORDER BY s.id;

-- Nombre d'utilisateurs par snack
SELECT 
    s.id AS snack_id,
    s.nom AS snack_nom,
    COUNT(u.id) AS nombre_utilisateurs
FROM snacks s
LEFT JOIN utilisateurs u ON s.id = u.snack_id
GROUP BY s.id, s.nom
ORDER BY s.id;

-- Nombre de commandes par snack
SELECT 
    s.id AS snack_id,
    s.nom AS snack_nom,
    COUNT(c.id) AS nombre_commandes
FROM snacks s
LEFT JOIN commandes c ON s.id = c.snack_id
GROUP BY s.id, s.nom
ORDER BY s.id;








