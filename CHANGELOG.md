# Changelog

## 1.2.0

### Added

- Stability Bonus: the item modifier field `stability` adds points to the Stability of every shield block the player makes. It can be negative; the Stability used for a block never goes below 0.
- Weight modifiers: the item modifier fields `flat_weight`, `weight_increase`, and `weight_reduction` change Effective Weight, which never goes below 0.
- Item tooltips show the new modifier lines, and `/stamina inspect` shows the Stability Bonus and the weight before modifiers.
- API 1.2: the same four fields for modifier providers (`StaminaContribution`).
- API 1.2: `IStaminaService.checkActionStart` checks whether an action may start now, with the normal refusal feedback, without a cost event or a payment.
- API 1.2: an item value provider can claim the bow-like use of its own items (`IItemValueProvider.ownsBowUse`); JASS then applies no bow-draw cost, refusal, or tooltip line to them. `JassApi.isJassBowDraw` and `JassApi.isBowUseOwnedByAddon` answer the same question for addons.
- API 1.2: `PreparedStaminaAction.getPayableAmount()`.

### Changed

- A held prepared discrete action now always pays its Payable Amount: its cost, limited to the room the debt floor leaves after the other open claims. While it is open, API spends, direct actions, shield blocks, and continuous settlements stop above that amount instead of using it, so near the debt floor they can cost less than before. Without an open prepared action nothing changes.

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
