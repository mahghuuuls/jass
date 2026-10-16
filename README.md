# Just Another Stamina System (JASS)

A server-authoritative stamina system for Minecraft 1.12.2. Melee swings, shield blocks, sprinting, bow draws, and optionally jumps and Elenai Dodge 2 dodges spend one stamina pool. Costs depend on the item, on armor and held weight, on shield Stability, and on item modifiers. Overspending leaves the player in stamina debt.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- License: [MIT](LICENSE). Build tooling notices: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

## Identity and installation

- Mod ID `jass`, version 1.0.0, Forge 1.12.2 artifact. Developed and tested on Cleanroom.
- Required on the server and on every client.
- Requires MixinBooter 10.7 or later (Cleanroom includes it).
- Optional integrations, each active only when the mod is present: Elenai Dodge 2 (1.1.0 or later), Baubles API providers such as Baubles or Bubbles (1.5.2 or later), Classic Bar Legacy (0.6.0 or later), RLCombat, Inhibited.

## Configuration

Server settings live in `config/jass/jass.cfg`. In multiplayer they are sent to clients, so tooltips show the server's values.

| Area | Keys |
| --- | --- |
| Pool | `max_stamina`, `stamina_regeneration`, `regeneration_delay`, `max_debt_fraction`, `efficiency_scale`, `min_discrete_cost`, `creative_consumption` |
| Movement | `sprint_cost`, `jump_cost_enabled`, `jump_cost`, `weight_factor` |
| Weight | `armor_weight`, `held_weight`, `fallback_weight_per_armor_point` |
| Melee and bows | `default_melee_cost`, `melee_cost_overrides`, `default_bow_draw_cost`, `bow_draw_cost_overrides` |
| Shields | `block_stamina_per_damage`, `stability_scale`, `shield_stability`, `default_shield_stability`, `shield_melee_block`, `shield_projectile_block`, `default_shield_melee_block`, `default_shield_projectile_block`, `guard_break_cooldown`, `shield_raised_regeneration` |
| Item modifiers | `item_stamina_modifiers`, `disabled_stamina_providers` |
| Integrations | `inhibited_integration`, `rlcombat_integration`, `elenai_dodge_resource_integration`, `elenai_dodge_base_cost`, `elenai_weight_source_integration`, `elenai_weight_conversion` |
| Operations | `command_permission_level`, `debug_logging` |

Client settings live in `config/jass/jass-client.cfg`: `hud_style` (`bar` or `segments`), bar position, size, offsets and fill direction, segment offsets, `hud_numeric_text`, `hud_hide_when_full`, `hud_hide_outside_inhibited`, `denied_action_sound`, `item_tooltips` (`always`, `shift`, `off`), the Classic Bar options, `hide_elenai_feather_hud`, and `hide_elenai_weight_tooltip`.

Per-item lists use `namespace:item[@metadata or @*]=value`, one entry per line. Item modifier rules use `namespace:item=field:value;field:value` and apply while the item is in an armor slot, a hand, or a Baubles slot.

Weight multiplies only sprint, jump, and dodge costs: `1 + Effective Weight x weight_factor`. Effective Weight is the worn armor weight plus the held weight of both hands. With Elenai Dodge 2 and `elenai_weight_source_integration`, the armor part comes from Elenai's own weights times `elenai_weight_conversion`; the held part always comes from `held_weight`.

Commands: `/stamina inspect [player]`, `/stamina restore [player]`, `/stamina set <player> <value>`, at `command_permission_level` (default 2).

## Compatibility notes

- ShieldBreak: both mods can put a shield on cooldown. They overlap and are not meant to be combined.
- Elenai Dodge 2: dodges stay subject to Elenai's own armor weight limit, which JASS does not change. JASS never edits Elenai's config; it hides Elenai's feather bar and armor weight tooltip only while the matching JASS features are active. Elenai 1.1.0 itself throws an error when its config is saved from its in-game screen while connected to a remote server; this happens in Elenai's code.
- RLCombat: off-hand attacks use the off-hand item's cost. Off-hand swings at air, Spartan Shields shield bashes, and Spartan Weaponry parries are free in 1.0.0.
- Classic Bar Legacy: JASS registers the bar `jassstamina` and never writes Classic Bar's config. Add the bar to a Classic Bar order to use it.

## API

Public types are in `com.mahghuuuls.jass.api` (common) and `com.mahghuuuls.jass.api.client` (client only). Entry point: `JassApi`.

- `JassApi.getStaminaService()`: read state and derived stats; spend, restore, or set stamina with a cause id; run discrete and continuous actions that follow JASS rules, with the item and hand behind them.
- `registerItemProvider` and `registerContextualProvider`: stat modifiers from items or from player context.
- `registerItemValueProvider`: default swing cost, bow drain, shield values, armor weight, and held weight for items without a config entry. Config entries win over providers.
- `prepareDiscreteAction` and `prepareContinuousTick` (API 1.1): check and price an action before its effect, then `settle()` it after the effect succeeds or `release()` it if the effect fails. Until then its cost is held as a Stamina Claim, so other actions treat it as spent; a preparation not settled or released by the end of the server tick is released. A continuous tick reserves its stamina, and its settlement reports `PAID_STOP` when the action must stop. Check `JassApi.API_VERSION` before calling them: JASS 1.0 does not have them.
- Events on the Forge bus: `StaminaCostEvent` (adjust or cancel a cost, with item and hand), `StaminaChangeEvent`.
- Client: `ClientStaminaApi` (stamina snapshot, item values), `StaminaHudRenderEvent` (replace the HUD), `StaminaTooltipEvent` (change the tooltip lines).

Providers register during loading; registration closes when loading completes.

## License

MIT. See [LICENSE](LICENSE).
