# AvertoxJobSystem

Minecraft job progression plugin (Spigot 1.20.4 API) with:
- 4 jobs: Farmer, Fisher, Woodcutter, Miner
- XP/level progression
- Active-job system (one job at a time, switch cooldown)
- Vault economy payouts and upgrade spending
- Owner-bound tiered job tools (stone start -> higher tiers)
- Admin testing panel (`/jobsadmin`) for live progression controls
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
- `/jobsadmin` or `/jobsadmin <player>`  
  Opens admin GUI controls (op or `avertoxjobs.admin` permission required).

## Admin GUI

The admin panel is designed for fast testing and balancing in-game.

- Player selection GUI
- Per-job context switching (Farmer/Fisher/Woodcutter/Miner)
- Give/take XP
- Give/take levels
- Give/take money + update plugin progress money
- Increase/decrease tool tier
- Reforge/give current bound tool
- Force active job
- Clear job switch cooldown
- Reset selected job progress (xp/level/money/upgrades/recipes/tool tier)

Permission:
- `avertoxjobs.admin` (default: op)

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

## Understanding the Plugin (Player Guide)

This section is for players who just want to know how things work without technical detail.

### What is XP?

XP is your progression score for a specific job.

- You gain XP by doing that job's actions.
  - Farmer: harvesting crops
  - Fisher: catching fish
  - Woodcutter: chopping logs
  - Miner: mining valid blocks for miner progression
- More XP means higher level in that job.

### What does level do?

Level unlocks stronger job features.

- Higher levels unlock stronger perks and mechanics.
- Level also lets you upgrade relic tiers further.
- Each job has its own level track (Farmer level is separate from Miner level).

### What are relics?

Relics are your job tools.

- Each job has its own relic type (hoe, rod, axe, pickaxe).
- Relics are bound to you and your selected job.
- Relics get stronger by upgrading tiers.
- Relic lore (item hover text) shows your perks.

### How do relic upgrades work?

Use the forge menu:

- Open `/jobs upgrade` (for active job) or `/jobs upgrade <job>`.
- Pay the upgrade cost.
- If your level is high enough, the relic ascends to the next tier.
- Higher tiers improve materials, names, and effectiveness.

### Why does my relic name/material change?

That is intended progression feedback.

- Tier increases evolve relic identity and quality.
- Visual changes help you feel progression.
- Mythic naming reflects power growth.

### Can I have multiple jobs active at once?

No.

- You can only work one active profession at a time.
- Change active job in `/jobs`.
- Switching to another job starts a cooldown timer.

### Do I lose progress when switching jobs?

No.

- You keep XP/level progress for each job.
- Switching changes your active profession only.

### What if I lose my relic?

If you lose it (death/drop/break), relic tier resets to Stone baseline for that job.

- You can retrieve/reforge your current relic from the forge menu.
- Non-owners cannot use your relic for progression.

### Why am I not getting XP/money sometimes?

Common reasons:

- Wrong active job selected.
- Wrong relic for that job.
- Relic not in main hand.
- Block was player-placed and blocked by anti-exploit rules.
- Job action does not qualify for reward in that context.

### How does money work?

- Valid job actions reward money.
- Some jobs have special bonus events (for example special fish catches).
- Tool tier and progression can increase earnings.
- Economy still follows server-side balance constraints and plugin settings.

### Why is anti-exploit needed?

To keep progression fair.

- Place-break loops (placing your own farm blocks and re-breaking) are blocked.
- Rewards are intended for genuine gameplay actions.

### Quick Start (New Player)

1. Run `/jobs`.
2. Click the job you want.
3. Receive your relic tool.
4. Do that job's actions to gain XP and money.
5. Upgrade relic tier in forge when available.
6. Repeat and grow stronger.

## Progression Deep Dive

This section explains how the full progression loop works in practice, from choosing a job to endgame relic tiers.

### 1) Core Loop

The plugin progression loop is:

1. Choose or switch active job in `/jobs`.
2. Receive the bound relic/tool for that job.
3. Perform job-specific actions (harvest, fish, chop, mine).
4. Gain XP and money for that job.
5. Level up that job and unlock stronger effects.
6. Upgrade relic tier in the forge menu (`/jobs upgrade <job>` or `/jobs upgrade` for active job).
7. Repeat for higher tiers and higher levels.

### 2) Active Job System

- A player can only have one active job at a time.
- You pick active job from the Job Overview menu.
- Switching to a different job starts a cooldown (24 hours).
- You can still view progress for all jobs, and previously earned XP is kept.
- Cooldown affects switching only, not earning within your current active job.

Practical result:
- You can specialize for a day, then switch next day, without losing prior grind.

### 3) Job XP and Levels

- Each job stores its own `level` and `xp`.
- XP thresholds are config-driven per job.
- When XP crosses the next threshold:
  - Level increases.
  - Level-up celebration triggers (sound + safe visual burst).
  - New mechanics can unlock (depending on job level design).

Important:
- Levels are per-job. Farmer level does not increase Miner level.

### 4) Relics / Bound Tools

Each job uses a bound relic tool:

- Farmer relic (hoe progression)
- Fisher relic (rod progression)
- Woodcutter relic (axe progression)
- Miner relic (pickaxe progression)

Properties:
- Owner-bound (locked to that player).
- Job-bound (a miner relic does not work for farmer progression).
- Tiered from early material stages into high-end stages.
- Name/lore are mythic-themed and evolve each tier.
- Perk text is shown directly in item hover lore.

If lost:
- Losing a bound relic (drop/death/break) resets that relic tier to Stone baseline.
- Player can reforge/retrieve from the forge menu.

If stolen:
- Other players cannot use it for progression.
- Non-owner interaction turns it into a useless relic state.

### 5) Relic Tier Upgrades

Relic tiers are independent from base job level, but gated by level:

- You can only upgrade to next relic tier if your job level is high enough for that tier.
- Upgrade cost is economy-based and scales by tier.
- Upgrading:
  - Increases relic tier.
  - Reforges a new version of the relic with upgraded identity/perks.
  - Updates effective progression strength multipliers.

Upgrade menu intent:
- Tier progression is not cosmetic only; it materially increases performance and earnings.

### 6) Money and Reward Behavior

Money and XP are produced from actual job actions while active.

Examples:
- Farmer: crop actions (with farmer-specific balancing multipliers).
- Fisher: catches, rarity outcomes, and special fish bonuses.
- Woodcutter: valid log interactions.
- Miner: ore-focused economy (not general stone economy for money).

Key balancing points:
- Some jobs use stronger multipliers at higher tiers.
- Job-specific modifiers stack with tier effects and level unlocks.
- Economy scaling is influenced by config multiplier settings.

### 7) Anti-Exploit Rules

To prevent farmable loops:

- Player-placed crops/logs/ores/stones are tracked.
- Breaking those tracked blocks does not grant XP/money.
- This blocks place-break-repeat abuse.

Design goal:
- Reward real gameplay progression, not synthetic block loops.

### 8) Farmer-Specific Progression Notes

Current farmer behavior includes:

- Reduced baseline XP/money rate for economy stability.
- Crop outputs go straight to inventory.
- Regrowth returns mature crops.
- Right-click crop generation is disabled (to avoid infinite generation abuse).
- TNT harvest mechanic:
  - 3-block radius crop clear,
  - drops sent to inventory,
  - bonus money awarded.

### 9) Fisher-Specific Progression Notes

Current fisher behavior includes:

- Shorter wait windows as progression improves.
- Better visual/audio feedback while fishing.
- Rarity-weighted catches.
- Special fish chance for extra money events.
- Relic tiers improve fishing output and catch quality potential.

### 10) Admin Testing Controls (How It Maps to Progression)

The admin GUI is intentionally comprehensive for balancing:

- XP/level edits per selected job
- Money/progress edits
- Tool tier edits
- Force active job
- Cooldown clear
- Full selected-job reset

This allows tuning the full progression lifecycle quickly without code changes.

### 11) Practical Player Journey (Example)

1. Player clicks Miner in `/jobs`.
2. Miner becomes active; miner relic is granted.
3. Player mines valid miner targets and earns Miner XP/money.
4. Miner level rises; tier upgrade unlocks.
5. Player opens forge and upgrades relic to next tier.
6. Relic name/material/perks improve; mining output increases.
7. Player later switches to Fisher (after cooldown), keeping Miner progress.

This is the intended long-term progression cadence.

## Notes

- Vault economy integration is hooked at runtime (no compile-time Vault API dependency).
- Plugin is currently built against Spigot API `1.20.4-R0.1-SNAPSHOT` and Java 17.

## Developer & Rights

Developed by `TrulyKing03`
All rights reserved.
Email: `TrulyKingDevs@gmail.com`
