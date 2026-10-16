# Changelog

## 1.1.0

### Added

- API 1.1, prepared actions: an addon can check and price an action before its effect, then pay when the effect succeeds or cancel it at no cost (`prepareDiscreteAction`). Until then the cost is held as a Stamina Claim, so other actions treat it as spent.
- API 1.1, prepared continuous ticks: an addon can reserve one tick of stamina before the tick's effect (`prepareContinuousTick`). Other drains cannot use the reserve, and the result says when the action must stop.
- `JassApi.API_VERSION` reports the installed API version at run time.

### Fixed

- A failing addon stamina change listener now gets JASS's warning in the log; the payment still stays.
- A rounding sliver at the end of the regeneration delay no longer lets a held sprint key start sprint with empty stamina and restart the delay.

## 1.0.0

### Added

- Stamina pool with regeneration after a short delay and stamina debt.
- Stamina costs for melee swings (hit or miss), shield blocks, sprinting, and bow draws, with an optional jump cost.
- Per-item swing costs and bow drains, shield Stability, and partial blocks for melee and projectiles.
- Guard Break: a block that empties stamina lowers the shield for a short time.
- Slower stamina regeneration while a shield is raised (20 percent of the normal speed by default).
- Weight from worn armor and from the items in both hands, raising sprint, jump, and dodge costs.
- Configurable item modifiers for maximum stamina, regeneration, regeneration delay, and efficiency.
- Stamina HUD as a bar or a row of segments, with position, size, and visibility options.
- Stamina values in item tooltips, using the server's values in multiplayer.
- Commands: `/stamina inspect`, `/stamina restore`, `/stamina set`.
- Public API for addons: stamina state and actions, modifier providers, item value providers, cost and change events, HUD and tooltip events.

### Compatibility

- Elenai Dodge 2: dodges cost stamina instead of feathers; armor weight can come from Elenai's weights; Elenai's feather bar and armor weight tooltip are hidden while JASS replaces them.
- RLCombat: off-hand attacks cost the off-hand item's swing cost.
- Inhibited: stamina is spent only while the player has the Inhibited effect.
- Baubles and Bubbles: item modifiers apply to bauble slots.
- Classic Bar Legacy: stamina can be shown as the Classic Bar bar `jassstamina`.
- Requires MixinBooter 10.7 or later.
