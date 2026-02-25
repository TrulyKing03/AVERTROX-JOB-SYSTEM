<h1 align="center">?? AvertoxJobSystem</h1>
<p align="center">
  <b>Advanced Jobs Progression System for Spigot/Paper 1.20.4</b><br>
  Developed by <b>TrulyKing03</b> • All Rights Reserved
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.0.0-brightgreen.svg?style=for-the-badge" alt="Version"/>
  <img src="https://img.shields.io/badge/API-Spigot%201.20.4-blue?style=for-the-badge&logo=minecraft" alt="Spigot API"/>
  <img src="https://img.shields.io/badge/Language-Java%2017-orange?style=for-the-badge&logo=openjdk" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Database-MySQL-informational?style=for-the-badge" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge" alt="Project Status"/>
  <img src="https://img.shields.io/badge/License-Private-red?style=for-the-badge" alt="License"/>
</p>

---

## ?? Overview

**AvertoxJobSystem** is a full RPG-style jobs framework with progression, relic tools, economy rewards, automation blocks, anti-exploit protections, and job-locked recipes.

Players choose one active profession, grind XP and money through valid gameplay, unlock stronger mechanics, evolve their relic tiers, and expand into endgame automation.

---

## ?? Feature Showcase

| Type | Highlights |
|------|-------------|
| ?? **Job System** | 4 jobs: Farmer, Fisher, Woodcutter, Miner |
| ?? **Progression** | Per-job XP/level tracks with configurable thresholds |
| ?? **Relic Tools** | Owner-bound, tiered job tools with evolving perks |
| ?? **Economy** | Vault payouts and upgrade costs |
| ?? **Admin Toolkit** | `/jobsadmin` GUI for complete progression testing |
| ?? **Automation** | Per-job passive blocks at level 10 |
| ?? **Recipes** | Job-restricted unlockable crafting recipes |
| ??? **Anti-Exploit** | Blocks place-break farming loops |
| ??? **Persistence** | MySQL-backed player and automation data |
| ?? **GUI Suite** | Overview, upgrade/anvil, recipes, and automation collection menus |

---

## ?? Requirements

- Java 17
- Maven 3.8+
- Spigot/Paper 1.20.4-compatible server
- MySQL 8+
- Vault + economy provider (EssentialsX Economy, CMI, etc.)

---

## ?? Build

```bash
mvn clean package
```

Output jar:
- `target/AvertoxJobSystem-1.0.0.jar`

---

## ?? Installation

1. Put the jar in your server `plugins/` folder.
2. Ensure Vault and an economy provider are installed.
3. Start server once to generate `plugins/AvertoxJobSystem/config.yml`.
4. Edit MySQL credentials/settings.
5. Restart server.

---

## ?? Configuration

Main config source: `src/main/resources/config.yml`  
Runtime copy: `plugins/AvertoxJobSystem/config.yml`

Config includes:
- MySQL connection settings
- Autosave interval (`autosave_minutes`)
- Economy multipliers
- Per-job level thresholds
- Upgrade costs
- Crop/ore regrowth timers
- Fisher rarity rates
- Automation limits and generation timers
- XP/money rewards per action

---

## ?? Commands

- `/jobs`
  - Opens Job Overview Menu
  - Click a job to learn/select active profession

- `/jobs upgrade <job>`
  - Opens upgrade/anvil menu (`farmer`, `fisher`, `woodcutter`, `miner`)
  - Upgrade relic tier, inspect perks, reforge/retrieve relic

- `/jobs recipes <job>`
  - Opens recipe unlock menu for selected job

- `/jobsadmin` or `/jobsadmin <player>`
  - Opens admin panel (op or `avertoxjobs.admin`)

---

## ?? Admin GUI Controls

- Player selection
- Per-job context switch
- Give/take XP
- Give/take levels
- Give/take money (+ plugin progression money)
- Increase/decrease tool tier
- Reforge/give bound relic
- Force active job
- Clear switch cooldown
- Reset selected job progress (`xp`, `level`, `money`, `upgrades`, `recipes`, `tool tier`)

Permission:
- `avertoxjobs.admin` (default: op)

---

## ?? Active Job Rules

- Only one active profession at a time
- Click a job in `/jobs` to activate it and receive its tool
- Job switching has a cooldown (24h)
- Switching jobs does not delete other-job progression

---

## ?? Bound Relic Progression

Every job uses a personal, owner-bound relic tool.

- Starts at Stone tier
- Upgrades to higher tiers (up to tier 10)
- Hover lore shows:
  - custom tier name
  - tier number
  - job class
  - current perk summary

Behavior rules:
- Must hold the correct job relic in main hand for progression
- Lost relic (death/drop/break) resets that relic tier to Stone
- Non-owner relic use is blocked/useless
- Missing relic can be reforged from upgrade menu

---

## ?? Automation Blocks

Unlocks at level 10+:
- `HAY_BLOCK` -> Auto-Farm
- `BARREL` -> Auto-Fish
- `OAK_WOOD` -> Auto-Wood
- `BLAST_FURNACE` -> Auto-Mine

- Right-click owned block to collect resources
- Per-job block limits are config-driven

---

## ??? Data Storage (MySQL)

Auto-created tables:

- `jobs_table`
  - `uuid`, `job`, `level`, `xp`, `recipes`, `money_earned`, `upgrades`
- `automation_table`
  - `uuid`, `job`, `block_location`, `level`, `stored_items`

Data lifecycle:
- Load on join
- Save on quit
- Autosave on interval
- Save on shutdown

---

## ?? Job Progression Highlights

### ?? Farmer
- Reduced XP/money baseline for economy balance
- Drops go directly to inventory
- Regrowth returns fully grown crops
- Right-click crop generation disabled
- TNT auto-harvest in 3-block radius with money reward
- Relic tier scales farming rewards

### ?? Fisher
- Basic income early game
- Rod efficiency + rare chance scaling
- Higher-value fish outcomes at higher levels
- Faster reeling and bonus XP for rare catches
- Auto-Fishing unlock at level 10 (`BARREL`)
- Better fishing feel (timing, particles, sounds)
- Special fish bonus money events

### ?? Woodcutter
- Standard chopping early game
- Tree-felling unlock mid progression
- Higher speed + better durability efficiency at later levels
- Auto-Wood unlock at level 10 (`OAK_WOOD`)

### ?? Miner
- Early normal mining progression
- Movement/mining boosts at level milestones
- Pickaxe upgrade effects in mid tiers
- Vein mining at high levels
- Auto-Mining unlock at level 10 (`BLAST_FURNACE`)
- Money rewards only from ores (including nether ores)

---

## ??? Anti-Exploit

- Tracks player-placed crops/logs/ores/stones
- Breaking tracked blocks gives no XP/money
- Prevents place-break progression abuse

---

## ?? Level-Up Effects

- Achievement-style level-up sound
- Celebration particles (non-damaging)

---

## ?? Understanding the Plugin (Player FAQ)

### What is XP?
XP is your progression score for a specific job. More XP -> higher level.

### What does level do?
Each new level unlocks stronger perks, mechanics, and higher relic upgrade access.

### What are relics?
Relics are your job tools (hoe/rod/axe/pickaxe). They are bound to you and scale by tier.

### How do upgrades work?
Use `/jobs upgrade <job>` (or active-job upgrade menu), pay cost, meet level requirement, and ascend relic tier.

### What are recipes?
Recipes are job-locked craftables. You unlock them in the recipe menu once your level requirement is met.

### Can I have multiple active jobs?
No. One active job at a time. You can switch later after cooldown.

### Do I lose progress if I switch jobs?
No. XP/levels/recipes remain saved per job.

### Why am I not getting XP or money?
Typical causes:
- wrong active job
- wrong relic/tool
- relic not in main hand
- player-placed block (anti-exploit)
- action not valid for that job context

---

## ?? How Systems Interact (Big Picture)

1. Choose active job in `/jobs`
2. Receive/use matching relic
3. Perform valid actions for XP + money
4. Level up and unlock stronger mechanics
5. Upgrade relic tiers for stronger scaling
6. Unlock recipes and craft better items
7. Reach endgame automation and long-term progression loops

---

## ?? Progression Deep Dive

### 1) Core Loop
Select job -> use relic -> perform job actions -> gain XP/money -> level up -> upgrade relic -> repeat.

### 2) Active Job Economy
Cooldown controls switching, not progression within current active job. Progress is stored per job permanently.

### 3) XP Threshold System
Each job has config-defined thresholds. On threshold reach, level increases and level-up effects fire.

### 4) Relic Tiering Model
Relic upgrades are level-gated and cost money. Tier affects identity, perks, and practical output scaling.

### 5) Reward Model
Job actions produce XP/money based on job context, tier effects, and configured multipliers.

### 6) Recipe Gating
Level gives eligibility; menu unlock makes recipe permanently available for that player profile.

### 7) Anti-Abuse Layer
Player-placed block tracking blocks synthetic loops so only genuine gameplay is rewarded.

---

## ?? Notes

- Vault is integrated at runtime
- Built for Spigot API `1.20.4-R0.1-SNAPSHOT`
- Java 17 target

---

## ?? Developer & Rights

Developed by **TrulyKing03**  
All rights reserved.  
Email: **TrulyKingDevs@gmail.com**

---

<p align="center">
  <sub><b>AvertoxJobSystem</b> • Designed and developed by TrulyKing03</sub>
</p>

