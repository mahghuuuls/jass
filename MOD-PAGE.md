JASS gives every player one stamina pool for combat and movement. Melee swings, shield blocks, sprinting, and drawing bows spend stamina, and stamina refills quickly after a short pause. Weapons, weight, and item modifiers change how much each action costs, and a configurable display shows the current amount.

JASS adds its own weight system. Worn armor and the items in your hands have weight, and heavier gear makes sprinting, jumping, and dodging cost more stamina.

This mod also reworks shields. Each shield has a Stability value that sets how much stamina a block costs, a shield can stop only part of a melee hit or a projectile, a block that empties stamina causes a Guard Break, and stamina regenerates more slowly while a shield is raised. The config lets you set the Stability and the melee and projectile block shares of each shield.

The default configuration is inspired by souls-like games: a small stamina pool that recharges quickly. The settings can also fit other stamina styles, such as Valheim or Skyrim, with a larger pool that recharges more slowly. `max_stamina`, `stamina_regeneration`, and `regeneration_delay` control this.

There are other mods that add stamina to Minecraft 1.12.2, including [StaminaPlus](https://www.curseforge.com/minecraft/mc-mods/staminaplus) and [ER Core](https://www.curseforge.com/minecraft/mc-mods/er-core). Those work well for their intended scope. JASS focuses on one pool shared by combat and movement, with per-item tuning, for players, modpack creators, and addon developers who want detailed control over it.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

## For Players

### What Changes While Playing

- Every melee swing costs stamina, whether it hits or misses. Each weapon can have its own cost. Mining never costs stamina.
- Sprinting and drawing a bow drain stamina while you do them. Sprinting stops and a drawn bow is lowered without firing when stamina runs out.
- Stamina debt: a swing, block, or dodge goes through as long as you have any stamina left, even if it costs more than you have. The missing amount is taken as debt, and your stamina goes below zero.
- While stamina is at zero or below, you cannot swing, block, sprint, draw, or dodge.
- Example: you have 5 stamina and swing a sword that costs 12. The swing happens, and your stamina drops to -7. The display shows empty. After the short pause, stamina regenerates from -7 back up past zero, and only then can you act again. With the defaults this takes about one second. A last swing at low stamina is never wasted, but it costs you time.
- Stamina refills after a short pause once you stop spending it.
- Jumping can cost stamina too. This is off by default.

### Shields

- Blocking a hit costs stamina based on the damage of the hit.
- While a shield is raised, stamina regenerates at only 20 percent of its normal speed.
- Each shield has a Stability value. Higher Stability makes blocks cheaper.
- A shield can be set to stop only part of a melee hit or an arrow. The rest of the damage goes through. The vanilla shield stops everything by default.
- Guard Break: a block that takes your stamina to zero or below still works, but then the shield is lowered and cannot be raised for a short time. The shield shows a cooldown and plays the shield-break sound.
- At zero or below you cannot block, and the hit deals full damage.

### Weight

- Worn armor and the items in your hands have weight. Items in your inventory do not.
- More weight makes sprinting, jumping, and dodging cost more stamina. Each weight point adds 2 percent. Swing, bow, and block costs do not change with weight.
- Heavier armor weighs more. A full leather set weighs 6, and a full diamond set weighs 23.
- Shields and weapons add weight while held: a shield 4, a sword 2, an axe 3.
- Item tooltips show each item's weight.

### Display and Tooltips

The built-in display is a stamina bar above the hotbar, next to the other bars. It can instead be a row of lightning segments in the style of the hunger bar. It hides while stamina is full and flashes when an action is refused.

Item tooltips show the stamina values the server uses: swing cost, bow drain, shield Stability, block shares and block cost, weight, and item modifiers. Tooltips can be set to show only while Shift is held, or turned off.

### Optional Integrations

Each integration runs only when that mod is installed.

- [Elenai Dodge 2](https://www.curseforge.com/minecraft/mc-mods/elenai-dodge-2): dodges cost stamina instead of feathers, and the feather bar is hidden. Armor weight comes from Elenai's own weight settings and appears in the JASS weight tooltip line. Elenai still refuses dodges in armor that is too heavy for its own limit.
- [RLCombat](https://www.curseforge.com/minecraft/mc-mods/rlcombat): off-hand attacks cost the swing cost of the off-hand item.
- [Inhibited](https://www.curseforge.com/minecraft/mc-mods/inhibited), for example with [Combat Inhibited](https://www.curseforge.com/minecraft/mc-mods/combat-inhibited): stamina is spent only while you have the Inhibited effect. Outside it, nothing costs stamina and the display hides.
- [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles): item modifiers also apply to worn baubles.
- [Classic Bar Legacy](https://www.curseforge.com/minecraft/mc-mods/classic-bar-legacy): stamina can be shown as a Classic Bar bar instead of the JASS display. See Classic Bar Setup below.

[ShieldBreak](https://www.curseforge.com/minecraft/mc-mods/shieldbreak) is not meant to be used with JASS: both mods put shields on cooldown.

### Classic Bar Setup

Classic Bar does not add bars by itself, so you add the stamina bar once.

1. Open the Mods screen, select Classic Bar Legacy, and open its config.
2. Find `Bar right order` in its bar overlay options.
3. Add `jassstamina` to the list. Put it **last**.

Last is the best place because the stamina bar hides while stamina is full. Anywhere else, the bars after it move down while it is hidden and back up when stamina drops. To keep the bar on screen at all times, set `hud_hide_when_full` to `false` in `config/jass/jass-client.cfg`.

The bar color and the number beside the bar are set by `classicbar_bar_color` and `classicbar_show_numbers` in the same file. Icons and percentages are Classic Bar's own settings.

### Requirements

Required:

- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) `10.7` or newer (Cleanroom already includes it)

Install JASS on the server and on every client.

## For Modpack Creators

JASS is designed so that its default balance can be replaced without writing another mod. A pack can configure:

- Maximum stamina, stamina regeneration, the delay before regeneration resumes, and how deep stamina debt can go.
- The cost of melee swings, sprinting, bow draws, jumps, and Elenai dodges.
- Exact swing costs and bow drains for individual items.
- Shield Stability, melee and projectile block shares, block cost, Guard Break length, and how much regeneration is kept while a shield is raised.
- The weight of armor and of held items, and how strongly weight raises movement costs.
- Stamina properties granted by any registered item while worn, held, or equipped as a bauble.
- Whether creative players spend stamina, and which addon providers a server permits.
- The Elenai Dodge 2, RLCombat, Inhibited, and Classic Bar Legacy integrations.
- The local appearance and visibility of the stamina display and tooltips.

### Configuration Files

JASS generates two files:

- `config/jass/jass.cfg` controls the stamina pool, costs, per-item values, shields, weight, item modifiers, and integrations. In multiplayer, the server's file applies, and tooltips show its values.
- `config/jass/jass-client.cfg` controls the local display and tooltips.

Both can also be changed in game: Mod Options, JASS, Config, then General or Client.

Operators can use `/stamina inspect` to see a player's stamina, stats, weight, and the item rules that apply, `/stamina restore` to refill stamina, and `/stamina set` to set it.

## For Mod Developers

JASS includes a public API for addons. Addons can read and change stamina, add their own stamina actions, check an action before its effect and pay only when it succeeds, add stat modifiers, supply default stamina values for their items, adjust or cancel any cost, and replace the display or the tooltip lines. See the API section of the [source repository](https://github.com/mahghuuuls/jass).

## Detailed System Reference

### Stamina Stats

- **Stamina:** the player's current amount. It can go below zero (stamina debt).
- **Maximum Stamina:** the size of the pool. Default `60`.
- **Stamina Regeneration:** stamina restored per second once regeneration resumes. Default `25`.
- **Regeneration Delay:** how long regeneration waits after stamina is spent. Default `0.75` seconds.
- **Stamina Efficiency:** points that lower every cost with diminishing returns. Base `0`.
- **Debt limit:** stamina never goes below `-max_debt_fraction x Maximum Stamina`. Default `1.0`, so down to `-60`.

Stamina Efficiency is not a percentage. With `efficiency_scale` `K` (default `100`), every cost is multiplied by:

```text
K / (K + Stamina Efficiency)
```

So `100` efficiency halves every cost, and `200` makes everything cost one third. A one-time cost above zero never drops below `min_discrete_cost` (default `1`).

### Default Costs

```properties
stamina {
    D:default_melee_cost=12.0
    D:sprint_cost=5.0
    D:default_bow_draw_cost=8.0
    B:jump_cost_enabled=false
    D:jump_cost=12.0
    D:elenai_dodge_base_cost=10.0
}
```

Melee and jump costs are paid once per action. Sprint and bow costs are per second. Per-item swing costs and bow drains use one entry per line:

```properties
stamina {
    S:melee_cost_overrides <
        minecraft:diamond_sword=16
        minecraft:stick=4
     >
    S:bow_draw_cost_overrides <
        minecraft:bow=6
     >
}
```

A cost of `0` makes that item free. Anything drawn like a bow uses the bow rule, including modded longbows and aimed crossbows.

### Shield Blocks

A block costs:

```text
damage x block_stamina_per_damage x stability_scale / (stability_scale + Stability)
```

With the defaults (`block_stamina_per_damage` `1.0`, `stability_scale` `100`) and the vanilla shield's Stability of `50`, a block costs `0.67` stamina per point of damage. A shield without a Stability entry uses `default_shield_stability` (default `0`), so it pays the full damage.

Block shares set how much of a hit a shield stops: `1.0` stops everything, `0.7` lets 30 percent through. Melee hits and projectiles have separate lists:

```properties
stamina {
    S:shield_stability <
        minecraft:shield=50
     >
    S:shield_melee_block <
        minecraft:shield=1.0
     >
    S:shield_projectile_block <
        minecraft:shield=1.0
     >
    D:guard_break_cooldown=2.0
    D:shield_raised_regeneration=0.2
}
```

`shield_raised_regeneration` is the share of Stamina Regeneration kept while a shield is raised: `0.2` keeps 20 percent, `0` stops regeneration while blocking, and `1` keeps the full speed. The pause before regeneration still counts down while the shield is up.

### Weight

Weight multiplies only sprint, jump, and dodge costs:

```text
1 + Effective Weight x weight_factor
```

With the default `weight_factor` of `0.02`, each weight point adds 2 percent. Effective Weight is the weight of the worn armor plus the weight of the items in both hands.

Default armor weight per full set: leather `6`, gold `11`, chainmail `11`, iron `17`, diamond `23`. Armor without an entry weighs its armor points. Default held weight: shield `4`, every vanilla sword `2`, every vanilla axe `3`, everything else `0`. For example, full iron armor with a sword and a shield weighs `23`, so sprinting costs 46 percent more.

With Elenai Dodge 2, the armor part comes from Elenai's weight settings times `elenai_weight_conversion` (default `1.4`), and the held part still comes from the JASS `held_weight` list.

### Adding Stamina Properties to Existing Items

The `item_stamina_modifiers` list can give stamina properties to any registered item without writing an addon:

```text
namespace:item[@metadata-or-*]=field:value;field:value
```

The item counts only while worn as armor, held in either hand, or equipped as a bauble. For example:

```properties
stamina {
    S:item_stamina_modifiers <
        minecraft:golden_chestplate=flat_maximum:20;flat_delay:0.5
        minecraft:feather=efficiency:50
     >
}
```

The chestplate gives `+20 Maximum Stamina` and `+0.5 s Stamina Regeneration Delay` while worn. The held feather gives `+50 Stamina Efficiency`. The tooltip shows each line in blue or red.

Available fields:

- `flat_maximum`, `maximum_increase`, and `maximum_reduction`
- `flat_regeneration`, `regeneration_increase`, and `regeneration_reduction`
- `flat_delay`, `delay_increase`, and `delay_reduction`
- `efficiency`

Increases and reductions are fractions: `0.2` means 20 percent.

### Built-in Display Reference

The built-in display can:

- Be a bar (`hud_style=bar`) or a row of segments (`hud_style=segments`).
- Hide while stamina is full, and hide outside the Inhibited effect.
- Show no number, current stamina, maximum stamina, or both.
- Sit in the right or left bar column above the hotbar, or in any screen corner, with offsets.
- Change bar width and height, and fill left to right or right to left.

Source code: [github.com/mahghuuuls/jass](https://github.com/mahghuuuls/jass)
