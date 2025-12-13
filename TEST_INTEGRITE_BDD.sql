-- ============================================================
-- SCRIPT DE TEST ET VÉRIFICATION DE L'INTÉGRITÉ RÉFÉRENTIELLE
-- Version corrigée et optimisée pour MariaDB
-- ============================================================

-- Connexion à la base de données (à adapter selon votre configuration)
-- USE hakik_caisse_manager;

-- ============================================================
-- 1. VÉRIFICATION DES PRODUITS ORPHELINS
-- ============================================================
SELECT 
    'PRODUITS' AS table_name,
    p.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM produits p
LEFT JOIN snacks s ON p.snack_id = s.id
WHERE s.id IS NULL
GROUP BY p.snack_id;

-- ============================================================
-- 2. VÉRIFICATION DES UTILISATEURS ORPHELINS
-- ============================================================
SELECT 
    'UTILISATEURS' AS table_name,
    u.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM utilisateurs u
LEFT JOIN snacks s ON u.snack_id = s.id
WHERE u.snack_id IS NOT NULL AND s.id IS NULL
GROUP BY u.snack_id;

-- ============================================================
-- 3. VÉRIFICATION DES COMMANDES ORPHELINES
-- ============================================================
SELECT 
    'COMMANDES' AS table_name,
    c.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM commandes c
LEFT JOIN snacks s ON c.snack_id = s.id
WHERE s.id IS NULL
GROUP BY c.snack_id;

-- ============================================================
-- 4. VÉRIFICATION DES INGRÉDIENTS ORPHELINS
-- ============================================================
SELECT 
    'INGREDIENTS' AS table_name,
    i.snack_id AS snack_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM ingredients i
LEFT JOIN snacks s ON i.snack_id = s.id
WHERE s.id IS NULL
GROUP BY i.snack_id;

-- ============================================================
-- 5. VÉRIFICATION DES LIGNES DE COMMANDE (produit_id)
-- ============================================================
SELECT 
    'LIGNE_COMMANDE' AS table_name,
    lc.produit_id AS produit_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM ligne_commande lc
LEFT JOIN produits p ON lc.produit_id = p.id
WHERE p.id IS NULL
GROUP BY lc.produit_id;

-- ============================================================
-- 6. VÉRIFICATION DES LIGNES DE COMMANDE (commande_id)
-- ============================================================
SELECT 
    'LIGNE_COMMANDE' AS table_name,
    lc.commande_id AS commande_id_invalide,
    COUNT(*) AS nombre_occurrences
FROM ligne_commande lc
LEFT JOIN commandes c ON lc.commande_id = c.id
WHERE c.id IS NULL
GROUP BY lc.commande_id;

-- ============================================================
-- 7. RÉSUMÉ GÉNÉRAL DES INCOHÉRENCES
-- ============================================================
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
-- 8. STATISTIQUES PAR SNACK
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

-- ============================================================
-- 9. VÉRIFICATION COMPLÈTE DE L'INTÉGRITÉ (VUE D'ENSEMBLE)
-- ============================================================
SELECT 
    'VUE D''ENSEMBLE' AS type,
    COUNT(DISTINCT s.id) AS nombre_snacks,
    COUNT(DISTINCT p.id) AS nombre_produits,
    COUNT(DISTINCT u.id) AS nombre_utilisateurs,
    COUNT(DISTINCT c.id) AS nombre_commandes,
    COUNT(DISTINCT i.id) AS nombre_ingredients
FROM snacks s
LEFT JOIN produits p ON s.id = p.snack_id
LEFT JOIN utilisateurs u ON s.id = u.snack_id
LEFT JOIN commandes c ON s.id = c.snack_id
LEFT JOIN ingredients i ON s.id = i.snack_id;


