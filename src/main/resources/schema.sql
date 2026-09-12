-- Schéma de la base bb_enterprise.
-- Ajoute les CREATE TABLE IF NOT EXISTS ici ; chaque statement est séparé par ";".

CREATE TABLE IF NOT EXISTS bb_professions (
    code          INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    label         VARCHAR(64)   NOT NULL,
    max_per_group INT,
);

CREATE TABLE IF NOT EXISTS bb_ranks (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profession_code INT NOT NULL,
    code INT NOT NULL,
    label VARCHAR(64) NOT NULL,
    level INT NOT NULL
)

CREATE TABLE IF NOT EXISTS bb_companies (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    profession_code INT NOT NULL,
    owner_uuid VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW()
)

CREATE TABLE IF NOT EXISTS bb_company_members (
    company_id INT NOT NULL,
    player_uuid   VARCHAR(36) NOT NULL,
    rank_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT NOW()
)

CREATE TABLE IF NOT EXISTS bb_shop_offers (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profession_code INT NOT NULL,
    item_key INT NOT NULL,
    direction ENUM('buy','sell') NOT NULL DEFAULT 'buy',
    unit_price FLOAT NOT NULL,
    active BOOLEAN NOT NULL
)

CREATE TABLE IF NOT EXISTS bb_transactions (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    profession_code INT NOT NULL,
    item_key INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price FLOAT NOT NULL,
    direction ENUM('buy','sell') NOT NULL DEFAULT 'buy',
    created_at DATETIME NOT NULL DEFAULT NOW()
)