# AvertoxJobSystem

Minecraft job progression plugin (Spigot 1.20.4 API) with:
- 4 jobs: Farmer, Fisher, Woodcutter, Miner
- XP/level progression
- Vault economy payouts and upgrade spending
- MySQL persistence
- Automation blocks with passive generation
- Job-locked recipe unlocks and craft restrictions
- GUI menus for overview, upgrades, automation collection, recipes

## Requirements

- Java 17
- Maven 3.8+
- Spigot/Paper 1.20.4-compatible server
- MySQL 8+
- Vault + economy provider plugin (EssentialsX Economy, CMI, etc.)

## Build

```bash
mvn clean package
```

Output jar:
- `target/AvertoxJobSystem-1.0.0.jar`

## Install

1. Put the built jar in your server `plugins/` folder.
2. Ensure Vault and an economy provider are installed.
3. Start server once to generate `plugins/AvertoxJobSystem/config.yml`.
4. Edit MySQL credentials in config.
5. Restart server.

## Configuration

Main config: `src/main/resources/config.yml` (runtime copy in plugin folder).

Config includes:
- MySQL connection settings
- Autosave interval (`autosave_minutes`)
- Economy multipliers
- Per-job level thresholds
- Upgrade costs
- Crop/ore regrowth timing controls
- Fisher rarity rates
- Automation limits and generation timers
- Reward values (XP/money per action)

## Commands

- `/jobs`  
  Opens Job Overview Menu.
- `/jobs upgrade <job>`  
  Opens Upgrade/Anvil Menu for a job (`farmer`, `fisher`, `woodcutter`, `miner`).
- `/jobs recipes <job>`  
  Opens Recipe Unlock Menu for a job.

## Automation Blocks

Place these blocks at job level 10+ to create automation:
- `HAY_BLOCK` -> Auto-Farm
- `BARREL` -> Auto-Fish
- `OAK_WOOD` -> Auto-Wood
- `BLAST_FURNACE` -> Auto-Mine

Right-click owned automation block to open collection GUI.
Per-job max automation blocks per player is config-driven.

## Data Storage (MySQL)

Tables auto-created on startup:

- `jobs_table`
  - `uuid`, `job`, `level`, `xp`, `recipes`, `money_earned`, `upgrades`
- `automation_table`
  - `uuid`, `job`, `block_location`, `level`, `stored_items`

Player job data loads on join and saves on quit + autosave interval + shutdown.

## Implemented Progression Highlights

- Farmer:
  - Crop detection, configurable regrowth timer
  - TNT auto-harvest chance (configurable)
  - Level-gated speed/regrowth/automation hooks
- Fisher:
  - Level 1-3: basic fishing income
  - Level 4: improved rod efficiency (durability reduction) + boosted rare fish chance
  - Level 6: unlock higher-value fish conversions (rare/epic/legendary outcomes)
  - Level 8-9: faster reeling and rare/epic/legendary XP bonus
  - Level 10: Auto-Fishing block unlock (`BARREL`)
- Woodcutter:
  - Level 1-4: standard chopping
  - Level 5: tree-felling unlock (whole tree break)
  - Level 6-10: chopping speed boost + reduced axe durability cost
  - Level 10: Auto-Wood block unlock (`OAK_WOOD`)
- Miner:
  - Level 1-3: normal single-block mining
  - Level 4: movement and mining speed boosts
  - Level 5-7: pickaxe upgrade effects (extra rewards and bonus drop chance)
  - Level 8-10: vein mining unlock (connected ore breaking)
  - Level 10: Auto-Mining block unlock (`BLAST_FURNACE`)

## Notes

- Vault economy integration is hooked at runtime (no compile-time Vault API dependency).
- Plugin is currently built against Spigot API `1.20.4-R0.1-SNAPSHOT` and Java 17.
