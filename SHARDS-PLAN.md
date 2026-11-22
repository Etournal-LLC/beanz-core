# Shards in Hytale — Plan

Bring the Shards ruleset (`C:\dev\shards`) into Hytale as part of beanz-core. Kill a monster, take
its shard, have a Shardforger bind it into one of your body slots, and gain one of that monster's
abilities.

Drafted 2026-11-22. Nothing built yet.

---

## Ground rules

- **Shards stays the source of truth.** This mod *reads* Shards' data (`docs/ability-catalog.json`,
  `data/*.json`) and never edits the Shards repo. Hytale-only changes live here as an overlay file.
- **Shards is Etournal-only by its own rules (its decision 10).** This is a real-time Hytale
  adaptation of the idea. It doesn't change the ruleset.
- **beanz-core owns progression**, not MMO Skill Tree (Jonathan, 2026-11-22). What happens to MMO
  Skill Tree on the server is decided at BS7.
- **Build the building blocks once.** Shards writes every ability as a mix of 55 effect kinds
  (`data/effects.json`). The mod codes each kind once and reads the abilities as data, so it never
  hand-codes 960 abilities.
- **Target the server's Hytale version** (0.7.5, auto-update off).

## What carries over, and what doesn't

| Shards | Hytale version |
|---|---|
| 24 slots in 4 regions (Head, Body, Arms, Legs), 6 deep | Same |
| One shard = one ability, fixed at binding | Same |
| Focus and riders | Same (riders are data, already written: 2,880) |
| 20 monster types | Mapped to Hytale mobs. Start with the ones that match |
| Gem ranks (Pearl to Diamond) | Same idea. Pearl only for the first slice |
| d100 rolls, AS vs DS, critical tables | **Dropped.** Hytale's own real-time combat does the hitting |
| Ability ranks bought with training points | Kept. How players earn the points is an open decision (BS6) |
| Skill checks, "go first" bonuses, crit severity (118 abilities) | Left out until they get a real-time redesign |

Effect sorting from 2026-11-22 (my judgment, not yet tested in-game): 623 abilities use only easy
building blocks, 219 use medium ones, and 118 need redesign.

## Phases

Reviewed 2026-11-22 after the first draft: the shard menu moved up from the Shardforger phase to its own
phase (BS2), and a clickable-menu test was added to the spike.

| Phase | What | Done when |
|---|---|---|
| **BS0 Spike** | Prove the six risky pieces in a throwaway test: (6) play a built-in particle and sound from our code, and list the ones the game ships. Also (1) a mob drops a custom item, (2) how a player fires *several* active abilities, since Hytale fires abilities from the held item and the main hand overrides the off hand, (3) apply a buff, a damage over time and a status effect from code, (4) save a list per player, (5) a menu with buttons the server reacts to. Today's `BeanzMenuPage` only displays; it has no clicks | A short findings note in `docs/hytale/`, and a ruling on the ability-key scheme |
| **BS1 Shard core** | Load the Shards data at startup. Build the per-player shard body (24 slots, regions, depth, Focus) and save it. Admin command `/shard give <type>` for testing | Shard data survives a relog |
| **BS2 Shard menu** | The player-facing menu (design below). It reads names, descriptions and riders straight from the Shards data, so it can be built before the abilities work | A player can open the menu, bind a shard into a slot and pick its ability, with no commands |
| **BS3 Building blocks** | Code the most-used easy effect kinds first: damage, status effect, buff, debuff, movement, damage reduction, damage over time, area zone, heal, immunity, resistance. Build the look-and-sound presets alongside (see Spell effects) | Each kind tested on its own in-game |
| **BS4 First slice** | 4 monster types × 4 regions × first 2 abilities = 32 abilities, Pearl versions with riders. Candidates: Greenskin (Trork), Hollow (skeletons), Crawler (spiders), Beast (wolves and bears) | All 32 work in-game when picked in the menu |
| **BS5 Shardforger and drops** | Shard drops on the four mob families. Shardforger NPC in a town opens the menu in binding mode | A player can go from kill to bound ability the way the rules describe |
| **BS6 Ranks** | Ability ranks 1-20, how points are earned in Hytale, and rank spending in the menu | Ranks change the numbers in play |
| **BS7 Server test** | Install beanz-core on the Beanzgaming server, play it with the group, and decide what happens to MMO Skill Tree | The group has played it, and the MMO Skill Tree decision is made |

After BS7: more monster types, the medium building blocks, the Onyx-to-Diamond versions and Shaping
(the seven peoples).

## The shard menu (BS2)

The menu is where a player builds their character, so it gets its own phase. Four screens:

| Screen | What the player sees and does |
|---|---|
| **Body** | The home screen. Four regions (Head, Body, Arms, Legs), six slots each. A filled slot shows its monster type, ability name and rank. The Focus region is marked. Open slots are clickable |
| **Bind** | Click an open slot. Pick one of the shards you carry, then pick one ability from that monster's list for that region. Each choice shows its description, passive or active, the stat it scales from, and the rider it would get from your current Focus. A confirm step warns that unbinding later is costly |
| **Ability** | Click a filled slot. Full description, current rank, what the next rank gives, and (from BS6) the button to buy a rank |
| **Codex** | Browse every monster type and its abilities by region, including ones you don't own, so players can plan a build before they bind anything |

Later additions: an **active bar** screen for choosing which active abilities sit on the keys (depends on the
BS0 key ruling), and the Shaping screen.

Where it opens: Body, Ability and Codex open anywhere (a key or `/shards`). Bind opens only at a
Shardforger once BS5 lands; until then it opens anywhere for testing.

## Spell effects (look and sound)

Hytale abilities can carry particles, trails, sounds, animations and camera effects (the `Effects` block on
an interaction, see `docs/hytale/interactions/interaction-reference.md`). beanz-core doesn't use any yet.

- **Presets, not one effect per ability.** Build one look-and-sound preset per damage type (Shards has 12:
  acid, cold, crush, electricity, fire, impact, mental, puncture, slash, spiritual, weapon, poison) and one per
  common effect (heal, shield, buff, debuff, teleport, summon, zone). About 20 presets cover all 960 abilities.
  An ability picks its preset from its data.
- **Use the game's own particles and sounds first.** Hytale ships these for its weapons, spells and mobs, and
  they are referenced by name. BS0 lists what's available and proves we can play one from our code.
- **Custom effects later.** Signature abilities can get their own look after the first slice. Check the
  Etournal asset store for sounds before making any.
- **Phase home:** proof in BS0, presets built alongside the building blocks in BS3.

## Open decisions

1. **Ability keys (decided at BS0).** Hytale has a few ability keys, and they belong to the held item.
   Passives (309 of 960) need no key. Options for actives: a few "equipped" actives on the keys, a
   shard item you hold, or a quick-select wheel.
2. **How ranks are earned (BS6).** Shards uses spent experience. In Hytale that could come from kills,
   use, or contracts.
3. **MMO Skill Tree (BS7).** Removing it also affects Perfect Parries, PartyPro and the Mastery Pack,
   which plug into it.
4. **Projects Hub entry.** The hub scripts only look for repos directly under `C:\dev`, and beanz-core sits
   one folder deeper (`C:\dev\hytale\beanz-core`). Its `.gitignore` also ignores `.claude/`, where
   `board.json` lives. Until both are fixed, this project doesn't appear on the hub.

## Decided

- **Unbinding is allowed, at a cost** (Jonathan, 2026-11-22). Shards itself makes a bound shard permanent.
  The Hytale version lets a Shardforger unbind one for a steep price, so a bad early pick doesn't ruin
  a character. The price is set in BS5.
