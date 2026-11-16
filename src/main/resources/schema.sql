-- Schéma de la base bb_enterprise.

CREATE TABLE IF NOT EXISTS bb_professions (
    code          VARCHAR(32)  NOT NULL PRIMARY KEY,
    label         VARCHAR(64)   NOT NULL
);

CREATE TABLE IF NOT EXISTS bb_ranks (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profession_code VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL,
    label VARCHAR(64) NOT NULL,
    level INT NOT NULL,
    UNIQUE KEY uq_profession_rank (profession_code, code),
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

CREATE TABLE IF NOT EXISTS bb_companies (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    profession_code VARCHAR(32) NOT NULL,
    owner_uuid VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE KEY uq_company_name (name),
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

CREATE TABLE IF NOT EXISTS bb_company_members (
    company_id INT NOT NULL,
    player_uuid   VARCHAR(36) NOT NULL,
    rank_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (company_id, player_uuid),
    FOREIGN KEY (company_id) REFERENCES bb_companies(id) ON DELETE CASCADE,
    FOREIGN KEY (rank_id) REFERENCES bb_ranks(id)
);

CREATE TABLE IF NOT EXISTS bb_shop_offers (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    profession_code VARCHAR(32) NOT NULL,
    item_key VARCHAR(64) NOT NULL,
    direction ENUM('buy','sell') NOT NULL DEFAULT 'buy',
    unit_price FLOAT NOT NULL,
    active BOOLEAN NOT NULL,
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

CREATE TABLE IF NOT EXISTS bb_transactions (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    profession_code VARCHAR(32) NOT NULL,
    item_key VARCHAR(64) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price FLOAT NOT NULL,
    direction ENUM('buy','sell') NOT NULL DEFAULT 'buy',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

CREATE TABLE IF NOT EXISTS bb_restricted_crafts (
    item_key        VARCHAR(64)  NOT NULL,
    profession_code VARCHAR(32)  NOT NULL,
    PRIMARY KEY (item_key, profession_code),
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

CREATE TABLE IF NOT EXISTS bb_restricted_drops (
    entity_type     VARCHAR(32)  NOT NULL,
    item_key        VARCHAR(64)  NOT NULL,
    profession_code VARCHAR(32)  NOT NULL,
    PRIMARY KEY (entity_type, item_key, profession_code),
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

CREATE TABLE IF NOT EXISTS bb_restricted_smelts (
    item_key        VARCHAR(64)  NOT NULL,
    profession_code VARCHAR(32)  NOT NULL,
    PRIMARY KEY (item_key, profession_code),
    FOREIGN KEY (profession_code) REFERENCES bb_professions(code)
);

-- Seed : catalogue des métiers (idempotent, sûr à rejouer à chaque démarrage)
INSERT IGNORE INTO bb_professions (code, label) VALUES
    ('MAIRIE', 'Mairie'),
    ('MEDECIN', 'Médecin'),
    ('AGRICULTEUR', 'Agriculteur'),
    ('ELEVEUR', 'Éleveur'),
    ('PECHEUR', 'Pêcheur'),
    ('RESTAURANT', 'Restaurant'),
    ('BARMAN', 'Barman'),
    ('ARMURIER', 'Armurier'),
    ('FORGERON', 'Forgeron'),
    ('BIBLIOTHECAIRE', 'Bibliothécaire'),
    ('SCULPTEUR', 'Sculpteur'),
    ('SCIERIE', 'Scierie'),
    ('ALCHIMISTE', 'Alchimiste'),
    ('FLIC', 'Flic'),
    ('FROMAGER', 'Fromager'),
    ('AGENCE_IMMO', 'Agence immo'),
    ('VIGNERON', 'Vigneron / bière');

INSERT IGNORE INTO bb_ranks (profession_code, code, label, level) VALUES
    ('MAIRIE', 'MAIRE', 'Maire', 2), ('MAIRIE', 'ADJOINT', 'Adjoint', 1), ('MAIRIE', 'CONSEILLER', 'Conseiller', 0),
    ('MEDECIN', 'CHEF', 'Chef', 2), ('MEDECIN', 'ADJOINT', 'Adjoint', 1), ('MEDECIN', 'RECRUE', 'Recrue', 0),
    ('AGRICULTEUR', 'CHEF', 'Chef', 2), ('AGRICULTEUR', 'ADJOINT', 'Adjoint', 1), ('AGRICULTEUR', 'RECRUE', 'Recrue', 0),
    ('ELEVEUR', 'CHEF', 'Chef', 2), ('ELEVEUR', 'ADJOINT', 'Adjoint', 1), ('ELEVEUR', 'RECRUE', 'Recrue', 0),
    ('PECHEUR', 'CHEF', 'Chef', 2), ('PECHEUR', 'ADJOINT', 'Adjoint', 1), ('PECHEUR', 'RECRUE', 'Recrue', 0),
    ('RESTAURANT', 'CHEF', 'Chef cuisinier', 2), ('RESTAURANT', 'ADJOINT', 'Serveur', 1), ('RESTAURANT', 'RECRUE', 'Commis', 0),
    ('BARMAN', 'CHEF', 'Chef', 2), ('BARMAN', 'ADJOINT', 'Adjoint', 1), ('BARMAN', 'RECRUE', 'Recrue', 0),
    ('ARMURIER', 'CHEF', 'Chef', 2), ('ARMURIER', 'ADJOINT', 'Adjoint', 1), ('ARMURIER', 'RECRUE', 'Recrue', 0),
    ('FORGERON', 'CHEF', 'Maître forgeron', 2), ('FORGERON', 'ADJOINT', 'Forgeron', 1), ('FORGERON', 'RECRUE', 'Apprenti', 0),
    ('BIBLIOTHECAIRE', 'CHEF', 'Chef', 2), ('BIBLIOTHECAIRE', 'ADJOINT', 'Adjoint', 1), ('BIBLIOTHECAIRE', 'RECRUE', 'Recrue', 0),
    ('SCULPTEUR', 'CHEF', 'Chef', 2), ('SCULPTEUR', 'ADJOINT', 'Adjoint', 1), ('SCULPTEUR', 'RECRUE', 'Recrue', 0),
    ('SCIERIE', 'CHEF', 'Chef', 2), ('SCIERIE', 'ADJOINT', 'Adjoint', 1), ('SCIERIE', 'RECRUE', 'Recrue', 0),
    ('ALCHIMISTE', 'CHEF', 'Chef', 2), ('ALCHIMISTE', 'ADJOINT', 'Adjoint', 1), ('ALCHIMISTE', 'RECRUE', 'Recrue', 0),
    ('FLIC', 'CHEF', 'Chef', 2), ('FLIC', 'ADJOINT', 'Adjoint', 1), ('FLIC', 'RECRUE', 'Recrue', 0),
    ('FROMAGER', 'CHEF', 'Chef', 2), ('FROMAGER', 'ADJOINT', 'Adjoint', 1), ('FROMAGER', 'RECRUE', 'Recrue', 0),
    ('AGENCE_IMMO', 'CHEF', 'Chef', 2), ('AGENCE_IMMO', 'ADJOINT', 'Adjoint', 1), ('AGENCE_IMMO', 'RECRUE', 'Recrue', 0),
    ('VIGNERON', 'CHEF', 'Chef', 2), ('VIGNERON', 'ADJOINT', 'Adjoint', 1), ('VIGNERON', 'RECRUE', 'Recrue', 0);
