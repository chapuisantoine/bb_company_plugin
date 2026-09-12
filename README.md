# BB Enterprise

Plugin Paper/Spigot (Kotlin, Minecraft 1.20.1) qui implémente un système de métiers/économie de rôle-play, complété à terme par un mod Forge pour les items custom.

> Ce fichier sert de mémo de conception. Il décrit les décisions d'architecture prises et le plan de développement, pas l'état réel du code (voir l'avancement dans les issues/commits).

## Métiers prévus

| Métier | Rôle |
|---|---|
| Médecin | Fabrique soins et bandages (1 par company) |
| Agriculteur | Seul habilité à cultiver, revend au Restaurant |
| Éleveur | Idem + vend le cuir au Bibliothécaire |
| Pêcheur | Idem (vente au Restaurant) |
| Restaurant | Achète les ingrédients, revend la nourriture aux joueurs |
| Barman | À définir |
| Armurier | Achète les métaux au Forgeron, revend armes/armures aux joueurs |
| Forgeron | Achète les minerais aux joueurs, les fait fondre |
| Bibliothécaire | Vend livres + enchantements (à voir), rachète cuir et canne à sucre, revend le sucre au Restaurant/Alchimiste |
| Scierie | Tout ce qui touche au build en bois |
| Alchimiste | Potions (idée : actif quand le Nether est ouvert) |
| Flics | À voir si faisable |
| Fromager | Craft + vend au Restaurant |
| Agence immo | À définir |
| Vigneron / bière | Craft + vend au Restaurant et au Bar |

Le système est pensé pour que l'ajout d'un nouveau métier reste une opération de configuration (une entrée `professions` + ses `shop_offers` + un listener spécifique si besoin), pas une refonte.

## Décisions d'architecture

- **Cohabitation plugin/mod** : un serveur Paper/Bukkit ne peut pas charger un mod Forge/Fabric nativement. Choix retenu : un **serveur hybride (Mohist ou Arclight, 1.20.1)**. Le plugin Bukkit/Kotlin actuel reste le foyer de toute la logique métier/économie ; un mod Forge séparé (non démarré) portera les définitions d'items custom (textures/modèles). Les joueurs devront installer ce mod côté client pour voir les vraies textures.
- **Companies** : système maison géré par BbEnterprise (pas de dépendance à Towny/Factions/SimpleClans), avec un quota par métier configurable par company (souvent 1, mais pas forcément pour tous les métiers).
- **Économie/argent** : déléguée à un plugin externe pas encore choisi, qui gère l'économie globale du serveur. BbEnterprise ne stocke pas les soldes lui-même ; les points d'achat/vente sont conçus derrière une interface (probablement branchée sur Vault) pour ne pas bloquer le développement en attendant ce choix.
- **Mods externes** : des mods de contenu (ex. Farmer's Delight pour la nourriture) seront ajoutés au fil de l'eau ; le mapping "produit d'un métier → item réel" doit donc rester découplé de la logique métier (voir couche `items` ci-dessous).

## Modèle de données (schema.sql, à ajouter)

Une company correspond à un seul métier : la rejoindre donne directement tous les droits de ce métier. Un joueur n'est membre que d'une seule company à la fois (donc un seul métier à la fois).

- `professions(code, label)` — catalogue des métiers
- `companies(id, name, profession_code, owner_uuid, created_at)` — une company rattachée à un métier précis
- `ranks(id, profession_code, code, label, level)` — les rangs sont propres à chaque métier (ex: Restaurant = Chef cuisinier/Serveur/Commis, Forgeron = Maître forgeron/Apprenti...) ; `level` est un entier qui donne l'ordre hiérarchique
- `company_members(company_id, player_uuid, rank_id, joined_at)` — `player_uuid` unique (un joueur = une seule company à la fois)
- `shop_offers(id, profession_code, item_key, direction[buy|sell], unit_price, active)`
- `transactions(id, player_uuid, profession_code, item_key, qty, unit_price, direction, created_at)` — journal d'audit

Les droits liés aux rangs restent codés en dur dans le plugin sous forme de seuils sur `level` (ex: inviter demande `level >= 1`), pas de colonnes de permissions en base — ça évite une migration de schéma à chaque nouvelle action, au prix de droits strictement cumulatifs par niveau (pas de rang "spécialisé").

Pas de quota (ni sur le nombre de membres d'une company, ni sur le nombre de companies par métier) pour l'instant — à réintroduire plus tard si besoin une fois le système testé.

## Découpage des packages Kotlin (proposé)

- `profession` — enum/table des métiers, service d'attribution et de vérification
- `company` — companies, appartenance, quotas
- `shop` — GUI d'achat/vente, consommateur des `shop_offers`
- `economy` — interface `EconomyProvider` (implé Vault branchée plus tard, stub/log pour l'instant)
- `listener` — events Bukkit qui font respecter les restrictions (récolte, pêche, élevage, craft réservé...)
- `items` — mapping entre "produit d'un métier" et l'ItemStack réel (vanilla en placeholder au départ, puis items du mod Forge)

## Plan de développement

1. Companies + attribution des métiers (commandes, tables, quotas)
2. Restrictions sur 2-3 métiers pilotes (Agriculteur pour la récolte, Pêcheur) pour valider le pattern d'event-listener avant de dupliquer sur tous les métiers
3. GUI de boutique avec items placeholder + économie no-op, pour valider toute la boucle achat/vente avant même d'avoir choisi le plugin d'économie
4. Intégration des mods de contenu externes (Farmer's Delight, etc.) dans les `shop_offers`
5. Setup du serveur hybride + démarrage du mod Forge pour les premiers items custom (soins/bandages)
6. Branchement du vrai plugin d'économie via Vault

## Mode de travail

Les modifications de code ne sont pas appliquées automatiquement dans ce projet : elles sont proposées en discussion, et c'est l'auteur du dépôt qui décide de ce qu'il intègre.
