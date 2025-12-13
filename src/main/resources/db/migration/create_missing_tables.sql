-- Script SQL pour créer les tables manquantes
-- À exécuter si les tables n'existent pas déjà

-- Table plans
CREATE TABLE IF NOT EXISTS plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL UNIQUE,
    prix_mensuel DOUBLE NOT NULL,
    description VARCHAR(1000),
    nombre_restaurants_max INT,
    nombre_utilisateurs_max INT,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- Table log_entries
CREATE TABLE IF NOT EXISTS log_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    level VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    username VARCHAR(100),
    snack_id BIGINT,
    details VARCHAR(1000),
    ip_address VARCHAR(50),
    user_agent VARCHAR(200)
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_log_snack_id ON log_entries(snack_id);
CREATE INDEX IF NOT EXISTS idx_log_timestamp ON log_entries(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_level ON log_entries(level);
CREATE INDEX IF NOT EXISTS idx_log_category ON log_entries(category);

-- Table promotions (si elle n'existe pas déjà)
CREATE TABLE IF NOT EXISTS promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    type_promotion VARCHAR(50) NOT NULL,
    valeur DOUBLE NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    code_promo VARCHAR(100),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    produit_id BIGINT,
    categorie VARCHAR(100),
    snack_id BIGINT NOT NULL,
    date_creation DATETIME NOT NULL,
    nombre_utilisations INT DEFAULT 0,
    nombre_utilisations_max INT
);

-- Table imprimantes (si elle n'existe pas déjà)
CREATE TABLE IF NOT EXISTS imprimantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    chemin VARCHAR(500) NOT NULL,
    snack_id BIGINT NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    largeur_papier INT DEFAULT 80,
    copies INT DEFAULT 1,
    impression_auto BOOLEAN DEFAULT FALSE,
    type_ticket VARCHAR(50) DEFAULT 'COMMANDE',
    date_creation DATETIME NOT NULL,
    date_modification DATETIME NOT NULL
);


