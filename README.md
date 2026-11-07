# Kazakiri – AIM Archangel (Re:AcademyCraft 1.5.0 add-on)

An Esper category inspired by Hyouka Kazakiri. Minecraft 1.20.1, Forge 47.4.10+, Re:AcademyCraft 1.5.0, **PlayerAnimator**.
A separate mod: it does not change Re:AcademyCraft. Install it on the server **and** every player's game.

## Required mods (server AND every player)
* Re:AcademyCraft 1.5.0
* **PlayerAnimator** for Minecraft 1.20.1 **Forge** (Modrinth/CurseForge: "playerAnimator" by KosmX).
  Players without it cannot join (the game will say it is missing).

## Status
| Stage | State |
|---|---|
| Written | Yes – 8 new abilities, arm/body poses, wings, halos, effects, camera, config |
| Syntax / API check | Passed in a sandbox (Minecraft/Forge/PlayerAnimator classes were not available there) |
| Compiled with Forge | **Not yet** – the GitHub build is the first real compile |
| Tested in Minecraft | **No** |

## The abilities (every skill: press to start, press again to end early)
| Skill | Level | What it does | Cost / cooldown (default) |
|---|---|---|---|
| Angel's Descent | 1 | Leap up on wings and glide (up to 7 s). Sprint = faster, sneak = drop faster. Landing pressure wave (4 blocks) | 500 CP / 14 s |
| AIM Revelation | 1 | 5 s: creatures within 28 blocks outlined through walls (hostiles rose, others cyan) – on your screen only | 400 CP / 20 s |
| Celestial Spear | 2 | Spear assembles above your hand, then flies straight 28 blocks, piercing up to 3 targets (12 dmg) | 900 CP / 8 s |
| Sanctuary | 2 | Wing shell around the ally under your crosshair (8 blocks) or yourself, 5 s, absorbs 30 damage; one hit of 14+ breaks it | 1200 CP / 22 s |
| Seraphic Array | 3 | 4 wing-shaped constructs behind you strike nearby enemies in turn for 5 s (7 dmg each) | 2200 CP / 28 s |
| Heaven's Rejection | 3 | Curved wave (9 blocks, 120°): damage, strong push-back, stops item use (bows, shields, eating), destroys projectiles | 1800 CP / 18 s |
| Archangel Protocol | 4 | 12 s transformation: second pair of wings, halo, Speed II; Spear ×1.3, Sanctuary ×1.25, Array +1 construct | 4500 CP / 75 s |
| AIM Convergence | 5 | 10 s ultimate (below) | 9500 CP / 150 s |

**AIM Convergence timeline:** 0–2 s rise, great halo and ground sigil · 2–4 s a giant Archangel forms behind you and opens
its wings · 4–6 s six constructs appear; enemies in the 14-block field are slowed and held · 6–8 s constructs strike ·
8.25 s the giant's wings beat down: one massive wave (18 dmg, push-back) · 8.5–10 s slow descent, everything fades.
You take 50% less damage during it, allies in the field 30% less.

All damage goes through Re:AcademyCraft's safety system, so PvP, teams and protected areas are respected; you and your
allies are never hit. Bosses resist push-back. No blocks are ever changed.
All numbers: `config/kazakiri-common.toml`. Visual options per player: `config/kazakiri-client.toml`
(cinematic camera on/off, screen flash on/off, own wings in first person).

## Visuals
All custom 3D geometry – **no particles**. Every skill also plays an arm/body pose through PlayerAnimator
(raised arm for the spear, arms forward for Sanctuary, a sweep for Rejection, arms spread for Protocol and Convergence…).
Convergence camera (caster only, optional): push-in, low angle, slow sideways drift, shake at the release, then back.

## Known limits (honest)
* The PlayerAnimator version in `gradle.properties` could not be checked offline. If the GitHub build fails on it,
  send the log and I will fix the version.
* The camera never takes control of your view; it only adds offsets.
* Flight permission is given only while a skill keeps you in the air, removed afterwards (also after a crash, on the
  next login), and never touches creative players. Your server does not need `allow-flight=true`.

## Build on GitHub
Replace all files in your repository with everything INSIDE this folder (keep `.github/workflows/build.yml`), commit.
**Actions** → green run → **Summary** → **Artifacts** → download **kazakiri-mod** → unzip → the `.jar` is the mod.
Red run → ⚙️ → **Download log archive** → send it back.

## Test (separate test instance, creative world, cheats on)
Mods folder: Re:AcademyCraft 1.5.0, PlayerAnimator (1.20.1 Forge), `kazakiri-forge-1.20.1-0.2.0.jar`. Then:
```
/aim cat kazakiri:aim_diffusion
/aim level 5
/aim learn_all
/aim fullcp
/summon zombie ~8 ~ ~
/summon skeleton ~10 ~ ~4
/summon zombie ~12 ~ ~-4
```
Put skills in your slots with the **Skills and presets** key, press **Toggle ability**, then the slot keys.
Between tests: `/aim cd_clear` and `/aim fullcp`. Press F5 to see poses and wings. Switch to survival
(`/gamemode survival`) to test Angel's Descent and Convergence flight properly.
