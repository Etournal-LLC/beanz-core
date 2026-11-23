# Shards BS0 spike: findings

Phase BS0 of `SHARDS-PLAN.md`. A throwaway test of the six risky pieces before any real Shards code is
written. Started 2026-11-22. Spike code lives in `src/main/java/com/beanz/core/spike/` and the
`Spike` asset folders; all of it is deleted or replaced in BS1.

Status key: **Proven** = checked by booting a real Hytale server with the mod. **Built, needs in-game check** =
code and assets load cleanly, but only a player in the game can confirm it behaves (see the test list at
the bottom). **Open** = needs a ruling.

---

## 0. Found first: beanz-core was not loading at all

Not one of the six pieces, but it blocked all of them.

- The game on this PC auto-updated to **Hytale 0.7.7** (server jar `Implementation-Version: 0.7.7`).
- 0.7.7 is compiled for **Java 25**. The mod was built for Java 21, so it would not compile.
- 0.7.7 replaced its vector maths class with JOML (`org.joml.Vector3d`, `.x()` instead of `.getX()`),
  removed `TransformComponent.getChunk()`, and `CustomUIHud` now needs an id string.
- The manifest's `ServerVersion` was a bare build string. Since at least 0.5.6 (June 2026) the server
  rejects that, so **beanz-core has not loaded in any world since then** (local server logs show
  `Failed to load manifest ... Bare version ... is not a valid range`).

Fixed in commit `8c8cd49`: build with Java 25, port the vector calls, `ServerVersion: ">=0.7.5 <0.8.0"`.
The mod now boots: `Enabled plugin com.beanz:BeanzCore` on a 0.7.7 server.

Build setup that now works (also in the build memory note):
- JDK 25: Temurin 25 unpacked at `%USERPROFILE%\.jdks\jdk-25.0.5.1+2` (no system install). Gradle 9.0 runs on it.
- `gradle/wrapper/gradle-wrapper.jar` is gitignored and was missing from this worktree; regenerate with the
  cached Gradle 9.0 (`gradle wrapper --gradle-version 9.0.0`).
- **Headless boot test** (new, very useful): the server jar runs on its own with
  `--assets <Assets.zip> --auth-mode offline`, using a `mods/` folder in its working directory. It loads the
  mod and validates every asset in about 15 seconds, without opening the game. `--generate-asset-schema <dir>`
  writes JSON schemas for every asset type; the five we need are in `docs/hytale/schema-0.7.7/`.

**Version gap:** the plan targets the server's 0.7.5. Only 0.7.7 exists on this PC, so everything here was
built and tested against 0.7.7. The 0.7.7 client did connect to beanzgaming.com on 2026-11-13, so the server
may already be newer than 0.7.5. Needs checking before BS7.

---

## 1. A mob drops a custom item — built, needs in-game check

- A death system (`SpikeShardDropSystem`, extends `DeathSystems.OnDeathSystem`, query `NPCEntity`) runs when
  any mob dies, reads its role name (`NPCEntity.getRoleName()`, e.g. `Trork_Warrior`, `Skeleton_...`,
  `Spider_Cave`, `Wolf_Black`, `Bear_Grizzly`) and spawns `Beanz_Spike_Shard` with
  `ItemComponent.generateItemDrop(...)` + `commandBuffer.addEntity(holder, AddReason.SPAWN)`.
- Doing it in code (rather than editing the game's 1,007 drop tables under `Server/Drops/NPCs/`) means the
  "which mob family drops which shard" mapping can come from data in BS5.
- All four first-slice families exist as mobs: Trork (Greenskin), Skeletons (Hollow), Spiders (Crawler),
  Wolves and Bears (Beast).
- The shard item inherits from the built-in `Ingredient_Crystal_Purple`, so it needs no art.

## 2. Firing several active abilities — partly answered, the key test is in-game

What the engine allows:
- Inputs are `Primary`, `Secondary`, `Ability1`, `Ability2`, `Ability3`, `Ability4`, `Use` (and others that
  are not keys). A key does nothing unless the **held item** maps an interaction to it.
- Every built-in weapon maps only `Primary`, `Secondary` and `Ability1` (its signature move); the crossbow
  also uses `Ability4`. **`Ability2` and `Ability3` are free on every weapon.**
- Earlier beanz-core testing (`docs/hytale/abilities/ability-system-behavior.md`) found the main hand
  overrides the off hand, but only tested the *same* key on both. Unknown: is the override per key (off hand
  still owns keys the weapon leaves empty) or whole-item?
- **MMO Skill Tree** (installed here) solves it with "cast items" that sit in hotbar slots 7-9: switch to the
  slot, left-click to cast. Proven, but every cast swaps you off your weapon.

Spike item `Beanz_Spike_Focus` maps Ability1-4 to one test interaction that says in chat which key fired.
Held in the off hand next to a sword, it shows directly whether the override is per key.

## 3. Buff, damage over time and status effect from code — proven in code, needs in-game check

This is the best news of the spike. Hytale's **entity effects** (`Server/Entity/Effects/`, 246 built in) are
data files that already cover most of the "easy" Shards effect kinds:

| Shards kind | Entity-effect field | Built-in example |
|---|---|---|
| damage over time | `DamageCalculator` + `DamageCalculatorCooldown` | `Burn`, `Poison_T1` |
| buff / debuff (damage) | `OutgoingDamage`, `DamageResistance` (per damage type, percent) | `Ability_Enrage_Buff` |
| stat changes (max health etc.) | `RawStatModifiers` | `Meat_Buff_T1` |
| slow / haste | `ApplicationEffects.HorizontalSpeedMultiplier` | `Slow` |
| stun / root | `MovementEffects.DisableAll`, `AbilityEffects.Disabled` | `Stun`, `Root` |
| immunity | effect + `ApplyConditions` `HasEffect` checks | `Immunity_Fire` |
| look and sound | `ApplicationEffects` tint, particles, sounds, screen effect | all of them |

- Applied from Java with `EffectControllerComponent.addEffect(ref, effect, seconds, OverlapBehavior, accessor, sourceRef)`.
  Duration can be set per call, so one asset can serve every rank.
- **The mod can author its own effects as data.** `Beanz_Spike_Bleed` (2 slashing damage per second for 6s)
  and `Beanz_Spike_Haste` (1.5x speed, +20% physical damage, 10s) load without warnings.
- For BS3: most building blocks become "generate an entity-effect asset from the Shards numbers, apply it".
  The full field list is in `docs/hytale/schema-0.7.7/EntityEffect.json`.
- Damage types: Hytale has Physical, Slashing, Bludgeoning, Crush, Projectile, Fire, Ice, Lightning, Poison,
  Earth, Water, Wind, Elemental. Ten of Shards' twelve map naturally. **Mental (6 uses) and spiritual (28) have
  no match**; damage causes are data files (`Server/Entity/Damage/`), so adding two is likely possible (BS3).
- Gotcha: built-in `Burn` only applies to players in Adventure mode, not Creative (`ApplyConditions`). Test in
  Adventure.

## 4. Save a list per player — built, needs relog check

`SpikeListComponent` is an entity component with a `STRING_ARRAY` codec, registered as `beanz:spike_list`.
Components registered with a codec are saved with the player automatically (same as the existing
`beanz:player_skills`). Needs the relog test to confirm.

## 5. A menu with buttons the server reacts to — built, needs in-game check

- `InteractiveCustomUIPage<Data>` + `UIEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
  "#BindButton", EventData.of("Action", "bind"))`; clicks arrive in `handleDataEvent(ref, store, data)`, and
  `sendUpdate(...)` changes the screen without reopening it.
- Pattern copied from the game's own `PrefabEditorExitConfirmPage` and its `.ui` file, using the game's
  shared button styles (`$C.@TextButton`, `$C.@SecondaryTextButton`).
- The **Bind** button writes to the saved list from piece 4, so one test proves menu and save together.
- Risk: `.ui` files are only read by the game client, so a markup mistake only shows when the page opens.

## 6. Built-in particles and sounds — built, lists made

- `ParticleUtil.spawnParticleEffect(id, position, accessor)` and
  `SoundUtil.playSoundEvent3d(SoundEvent.getAssetMap().getIndex(id), SoundCategory.SFX, position, accessor)`.
- The game ships **689 particle systems** and **1,569 sound events** (test folders excluded). Full lists:
  `docs/hytale/catalogs/particles.tsv`, `sounds.tsv`, `entity-effects.tsv`.
- Good preset candidates: `Effect_Fire`, `Effect_Poison`, `Impact_Critical`, `Buff_Enrage`, `Stunned`, and the
  `Spell/` (257) and `Combat/` (482) folders.

---

## Open: the ability-key scheme (ruling needed)

Two-thirds of Shards abilities are actives (651 of 960). A full 24-slot body would carry about 16 actives,
far more than any key layout holds, so every option needs a short list of "equipped" actives.

| Option | How it plays | Good | Bad |
|---|---|---|---|
| **A. Shard Focus in the off hand** | Ability2 and Ability3 (maybe Ability4) fire your equipped actives while you keep your weapon | Fast, real-time, weapon untouched | Only works if the in-game test shows the override is per key. 2-3 actives on keys |
| **B. Cast items on the hotbar** (MMO Skill Tree way) | Hotbar slots hold ability items: press 7, 8 or 9, then click | Proven by another mod. Up to 3+ actives | Swaps you off your weapon every cast. Eats hotbar slots. Clashes with MMO Skill Tree if both stay |
| **C. Quick-select wheel** | Press a key, a menu opens, pick the ability | Any number of actives | Hytale menus take over the screen and input. Too slow for combat |

How many actives sit on the keys is a separate number to set once the scheme is picked.

---

## In-game test list (Jonathan, about 10 minutes)

Open the `beanztest` world (BeanzCore is now switched on for it; old settings saved as
`config.json.before-bs0`). Type `/gamemode adventure` first.

1. `/spike menu` → click **Bind** twice. The yellow line should count up. Click **Close**.
2. `/spike give focus` → hold it in your **main hand**, press each ability key. Chat says which fired.
3. Put a sword in your main hand and the Focus in your **off hand**. Press the ability keys again.
   *This is the key test: does Ability2/Ability3 still fire from the off hand?*
4. `/spike effect Beanz_Spike_Haste` (you run faster), then `/spike effect Burn` (you take fire damage).
5. Stand near a mob: `/spike near Beanz_Spike_Bleed` and `/spike near Stun 5`.
6. `/spike fx Effect_Fire` and `/spike sound SFX_Effect_Burn_World`.
7. Kill a Trork, skeleton, spider, wolf or bear: a purple **Spike Test Shard** should drop.
   No such mobs nearby? `/spike dropall` makes every mob drop one.
8. Log out, log back in, `/spike list`. The entries from step 1 should still be there.

Then send the session log (`%APPDATA%\Hytale\UserData\Saves\beanztest\logs`, newest file) or just say what
happened at each step.

## Results

In-game test in `beanztest` on 2026-11-22 (Hytale 0.7.7):

| Step | Result |
|---|---|
| Mod loads | Yes (server log) |
| 1. Menu Bind | Clicks reach the server (`action=bind` twice in the log) |
| 2. Focus in main hand | Q (Ability1) and H (Ability4) fire. E and R do nothing |
| 2b. `/spike runes` (built-in ability slots) | **E fires `slot=Ability2` and R fires `slot=Ability3` while holding any item, with the spell particle.** Hytale's own ability-slot (rune) system works for mod abilities |
| 3. Off hand | Moot: E and R never read the held item in 0.7.7 |
| Lag | E and R lagged the game badly. Cause: `Effect_Fire` and `Buff_Enrage` are **looping** particles (some spawners have no `LifeSpan`, e.g. `Enrage_Particles`, 220 particles), built to ride on an entity effect. Spawned at a spot, each press left one running forever. Swapped for one-shot bursts (`Impact_Critical`, `Impact_Poison`). **Rule for BS3:** world-spawned ability visuals must be one-shot; looping visuals belong inside an entity effect so they stop when it ends |
| `/gamemode` | Refused in singleplayer (no operator rights). Built-in `Burn` needs Adventure mode, so test damage over time with `Beanz_Spike_Bleed` instead |
