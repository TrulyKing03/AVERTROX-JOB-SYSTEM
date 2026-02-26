# AvertoxJobSystem

Advanced jobs progression plugin for Spigot/Paper 1.20.4 (Java 17), backed by Vault + MySQL.

## What is included

- 5 jobs:
  - Farmer
  - Fisher
  - Woodcutter
  - Miner
  - Hunter
- Per-job XP, level thresholds, and money tracking
- Active-job switching with cooldown
- Bound relic tools with tier upgrades
- Job recipe unlock menus
- Automation blocks with upgradeable level
- Admin controls (`/jobsadmin`)

## New in this update

- Actionbar progression feedback:
  - Live XP progress bar
  - XP remaining to next level
- Jobs overview progress details:
  - Progress bar per job in `/jobs`
  - "XP to next level" shown in menu cards
- Sell-handler economy flow:
  - New `Job Handler` sell menu (`/jobs sell`)
  - Sells configured inventory items for Vault money
  - Block-break actions now grant XP only (no direct money)
- Automation upgrades:
  - Upgrade button in automation vault menu
  - Higher levels increase storage slots
  - Higher levels reduce generation interval (faster output)
  - Upgrade costs and max level support
- Miner speed behavior fix:
  - Mining speed boost now applies on block damage start, not only after block break
- Hunter job implementation:
  - New XP/money progression path from kills
  - New hunter relic tools and upgrades
  - Hunter appears in `/jobs`, upgrades, admin tools, recipes, and automation
- Fisher rarity fix:
  - Catch rarity now matches fish item outcome consistently

## Commands

- `/jobs`
  - Opens jobs overview
- `/jobs sell`
  - Opens sell-handler menu
- `/jobs upgrade <job>`
  - Opens relic upgrade menu
- `/jobs recipes <job>`
  - Opens recipe unlock menu
- `/jobsadmin` or `/jobsadmin <player>`
  - Opens admin controls

## Automation blocks

Placeable automation blocks (level 10 in matching job):

- `HAY_BLOCK` -> Farmer automation
- `BARREL` -> Fisher automation
- `OAK_WOOD` -> Woodcutter automation
- `BLAST_FURNACE` -> Miner automation
- `TARGET` -> Hunter automation

Right-click owned automation to open the vault menu, collect items, and upgrade.

## Economy model

- XP is still granted from valid job actions.
- Money from block-breaking jobs is now earned primarily through selling collected items in the sell menu.
- Fisher/Hunter still have direct action payouts plus item-based selling potential.

## Configuration highlights (`config.yml`)

- `jobs.<job>.level_thresholds`
- `jobs.<job>.upgrade_costs`
- `rewards.<job>.*`
- `sell_prices.<MATERIAL>`
- `automation.max_level`
- `automation.slot_base`
- `automation.slot_per_level`
- `automation.speed_upgrade_seconds`
- `automation.max_blocks_per_player.<job>`
- `automation.base_generation_seconds.<job>`

## Build

```bash
mvn clean package
```

Output artifacts are generated in `target/`, including:

- `AvertoxJobSystem-1.0.0.jar`
- `AvertoxJobSystem-1.0.0-shaded.jar`

## Requirements

- Java 17
- Maven 3.8+
- Spigot/Paper 1.20.4-compatible server
- MySQL 8+
- Vault + an economy provider

## Notes

- Data is persisted in MySQL (`jobs_table`, `automation_table`).
- Autosave interval is configurable (`autosave_minutes`).
- Anti-exploit placed-block tracking remains active for logs/ores/stone; farmer progression supports planted crops.
