# ?? AVERTROX JOB SYSTEM

> A premium-style Minecraft Jobs RPG plugin for Spigot/Paper 1.20.4

## ? Overview

**AvertoxJobSystem** transforms survival gameplay into a full progression experience with jobs, relic tools, upgrades, recipes, automation, and economy integration.

### Core Highlights
- ?? **4 Playable Jobs:** Farmer, Fisher, Woodcutter, Miner
- ?? **XP + Leveling** per job (independent progression tracks)
- ?? **Active Job System** (one active profession at a time)
- ?? **Vault Economy** rewards + upgrade spending
- ?? **Owner-Bound Relic Tools** with tier evolution
- ?? **Admin Testing GUI** (`/jobsadmin`)
- ??? **MySQL Persistence**
- ?? **Automation Blocks** for passive generation
- ?? **Job-Locked Recipe Unlocks**
- ?? **Advanced GUI Menus** for all systems

## ?? Requirements

- Java 17
- Maven 3.8+
- Spigot/Paper 1.20.4-compatible server
- MySQL 8+
- Vault + economy provider plugin (EssentialsX Economy, CMI, etc.)

## ??? Build

```bash
mvn clean package
```

Output jar:
- `target/AvertoxJobSystem-1.0.0.jar`

## ?? Installation

1. Place the built jar in your server `plugins/` folder.
2. Install Vault + an economy provider.
3. Start server once to generate plugin config files.
4. Edit MySQL credentials in `plugins/AvertoxJobSystem/config.yml`.
5. Restart server.

## ?? Configuration

Main config source: `src/main/resources/config.yml`  
Runtime config: `plugins/AvertoxJobSystem/config.yml`

Config supports:
- MySQL connection settings
- Autosave interval (`autosave_minutes`)
- Economy multipliers
- Per-job level thresholds
- Upgrade costs
- Crop/ore regrowth timers
- Fisher rarity rates
- Automation limits + generation timers
- Reward values (XP/money per action)

## ?? Commands

- `/jobs`
  - Opens Job Overview Menu
  - Click a job to learn/select it

- `/jobs upgrade <job>`
  - Opens forge/anvil menu for `farmer`, `fisher`, `woodcutter`, `miner`
  - Upgrade relic tier
  - Preview perks
  - Reforge/retrieve relic

- `/jobs recipes <job>`
  - Opens recipe unlock menu for that job

- `/jobsadmin` or `/jobsadmin <player>`
  - Opens admin test panel (op or `avertoxjobs.admin` required)

## ?? Admin GUI (Testing & Balancing)

Designed for live balancing without restarts:
- Player selection GUI
- Per-job context switching
- Give/take XP
- Give/take levels
- Give/take money + progress money edits
- Increase/decrease tool tier
- Reforge/give bound tool
- Force active job
- Clear job switch cooldown
- Reset selected job progress

Permission:
- `avertoxjobs.admin` (default: op)

## ?? Active Job Rules

- You can only work **one active profession** at a time.
- Click a job in `/jobs` to set it active.
- Switching jobs is on cooldown (24h default).
- Switching does **not** wipe progress from other jobs.

## ?? Relic Tools & Upgrades

Each job has a relic tool bound to its owner:
- Farmer ? Hoe relic
- Fisher ? Rod relic
- Woodcutter ? Axe relic
- Miner ? Pickaxe relic

Relic behavior:
- Starts at Stone baseline and upgrades through higher tiers.
- Tier affects material quality, name, perk strength, and output.
- Hover lore displays tier name, tier number, job class, and perk list.
- Requires correct relic in main hand for progression.

Security & loss behavior:
- Non-owner use = useless relic behavior.
- If owner loses tool (drop/death/break), tier resets to Stone baseline.
- Missing relic can be reforged/retrieved in upgrade menu.

## ?? Automation Blocks

Unlocked at level 10+ per job:
- `HAY_BLOCK` ? Auto-Farm
- `BARREL` ? Auto-Fish
- `OAK_WOOD` ? Auto-Wood
- `BLAST_FURNACE` ? Auto-Mine

- Right-click owned automation block to open collection GUI.
- Per-job automation limits are config-controlled.

## ??? Data Storage (MySQL)

Tables auto-created:

- `jobs_table`
  - `uuid`, `job`, `level`, `xp`, `recipes`, `money_earned`, `upgrades`

- `automation_table`
  - `uuid`, `job`, `block_location`, `level`, `stored_items`

Data lifecycle:
- Load on join
- Save on quit
- Autosave on interval
- Save on shutdown

## ?? Understanding the Plugin (Player-Friendly)

### What is XP?
XP is your job progression score. Doing valid actions in your active job grants XP.

### What does level do?
Levels unlock stronger job mechanics, better relic growth, and recipe progression.

### What are relics?
Relics are your personal job tools. They evolve by tier and are bound to you.

### What are recipes?
Recipes are job-based unlockables. You must meet level requirements and unlock them via menu before crafting.

### How progression loops together
1. Pick active job in `/jobs`
2. Use matching relic tool
3. Perform valid actions for XP + money
4. Level up and unlock stronger mechanics
5. Upgrade relic tiers in forge menu
6. Unlock recipes and craft stronger outcomes
7. Repeat to scale power and earnings

## ?? Job Progression Snapshot

### ?? Farmer
- Balanced lower XP/money rate
- Crop drops sent directly to inventory
- Fully grown regrowth behavior
- TNT 3-block harvest burst with money rewards

### ?? Fisher
- Faster and more engaging fishing flow
- Better VFX/SFX feedback
- Special fish for bonus money
- Higher levels improve rarity potential and returns

### ?? Woodcutter
- Tree-felling unlock at level milestones
- Better speed and durability efficiency at higher levels
- Auto-Wood unlock at endgame

### ?? Miner
- Ore-focused progression and economy rewards
- Movement/mining boosts at mid levels
- Vein mining unlock at higher levels
- Auto-Mining unlock at level 10

## ??? Anti-Exploit

To keep progression fair:
- Player-placed crops/logs/ores/stones are tracked.
- Breaking placed blocks gives no XP/money.
- Prevents place-break farming loops.

## ?? Level-Up Effects

- Achievement-style sound on level up
- Safe celebration particles (non-damaging)

## ?? Notes

- Vault economy integration hooks at runtime.
- Built against Spigot API `1.20.4-R0.1-SNAPSHOT` with Java 17.

---

## ?? Developer & Rights

**Developed by:** `TrulyKing03`  
**Contact:** `TrulyKingDevs@gmail.com`  
**Copyright:** © TrulyKing03. **All rights reserved.**
