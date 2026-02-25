# AvertoxJobSystem

Minecraft job progression plugin (Spigot 1.20.4 API) with:
- 4 jobs: Farmer, Fisher, Woodcutter, Miner
- XP/level progression
- Active-job system (one job at a time, switch cooldown)
- Vault economy payouts and upgrade spending
- Owner-bound tiered job tools (stone start -> higher tiers)
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
  Opens Job Overview Menu (click a job to learn/select it).
- `/jobs upgrade <job>`  
  Opens Upgrade/Anvil Menu for a job (`farmer`, `fisher`, `woodcutter`, `miner`) where players:
  - upgrade bound tool tier
  - preview perk changes
  - retrieve/reforge current tier tool
- `/jobs recipes <job>`  
  Opens Recipe Unlock Menu for a job.

## Active Job Rules

- Players can only work one profession at a time.
- Click a job in `/jobs` to set it active and receive the matching bound tool.
- Job switching is limited to once every 24 hours.
- Switching jobs does not delete previously earned XP for other jobs.

## Bound Tool Progression

- Every job uses an owner-bound progression tool.
- Tool progression starts at Stone tier and can be upgraded up to tier 10.
- Tool hover shows:
  - custom tier name
  - tier number
  - job class
  - perk list (scales by tier)
- Higher tiers scale rewards and performance significantly (speed, reward efficiency, job-specific bonuses).
- Job actions require holding the correct bound tool for that job.
- If the owner loses the tool (drop/death/break), tool tier resets back to Stone.
- If another player picks up someone else's bound tool, it becomes a useless broken relic.
- If a player has no bound tool, the system auto-grants it when they attempt job work.

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
  - Reduced XP/money rate for better economy balance
  - Crop drops go directly into player inventory
  - Crop regrowth returns fully grown plants
  - Right-click crop generation is disabled
  - TNT auto-harvest destroys crops in 3-block radius, stores drops directly, and rewards money
  - Tool tier scales crop XP/money gain
- Fisher:
  - Level 1-3: basic fishing income
  - Level 4: improved rod efficiency (durability reduction) + boosted rare fish chance
  - Level 6: unlock higher-value fish conversions (rare/epic/legendary outcomes)
  - Level 8-9: faster reeling and rare/epic/legendary XP bonus
  - Level 10: Auto-Fishing block unlock (`BARREL`)
  - Improved fishing feel: faster waits, water particle feedback, better catch audio
  - Special fish can be caught for extra money
  - Tool tier scales catch rewards and rarity weighting
- Woodcutter:
  - Level 1-4: standard chopping
  - Level 5: tree-felling unlock (whole tree break)
  - Level 6-10: chopping speed boost + reduced axe durability cost
  - Level 10: Auto-Wood block unlock (`OAK_WOOD`)
  - Tool tier scales chopping rewards and durability efficiency
- Miner:
  - Level 1-3: normal single-block mining
  - Level 4: movement and mining speed boosts
  - Level 5-7: pickaxe upgrade effects (extra rewards and bonus drop chance)
  - Level 8-10: vein mining unlock (connected ore breaking)
  - Level 10: Auto-Mining block unlock (`BLAST_FURNACE`)
  - Money only from ore blocks (including nether ores)
  - Tool tier scales ore rewards and bonus drop strength

## Anti-Exploit

- Player-placed crops/logs/ores/stones are tracked.
- Breaking those placed blocks gives no job XP/money.
- Prevents place-break farming loops.

## Level-Up Effects

- Plays a short achievement-style sound.
- Shows particle-based celebration burst (non-damaging).

## Notes

- Vault economy integration is hooked at runtime (no compile-time Vault API dependency).
- Plugin is currently built against Spigot API `1.20.4-R0.1-SNAPSHOT` and Java 17.
