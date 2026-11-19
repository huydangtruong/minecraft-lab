# P-058 Crop growth passive (D149)

Sixth passive in the Passives tab (emerald, hexagon cut). +20% growth per level, Lv10 = +200%. Switchable.

## Design
- `passive/CropOwners` (SavedData `permahearts:crop_owners`, per dimension): packed BlockPos -> planter UUID. The overworld copy also caches each owner's active growth level (refreshed on join, leave, upgrade, toggle) for offline owners.
- `mixin/CropPlantMixin`: `BlockItem.place` RETURN, a ServerPlayer that placed a crop records itself. `FarmTask` records the clone owner after replanting/planting.
- `mixin/CropGrowthTickMixin`: `BlockStateBase.randomTick` TAIL -> `CropGrowth.afterRandomTick` runs `PassiveRules.extraGrowth(level, roll)` extra random ticks (reentry guard). Only vanilla random ticks drive it, so crops grow only in loaded chunks.
- `PlayerBlockBreakEvents.AFTER` forgets the owner; a tick on a pos that is no longer a crop also forgets it.
- Crops: `CropBlock` (wheat, carrots, potatoes, beetroots), `StemBlock`, `CocoaBlock`, `NetherWartBlock`.
- `PassiveData` gains `growth_on`; `PassiveNet.TOGGLE` (old `TOGGLE_VISION`) takes any `Passive.toggleable()`.
- Tab: six rows (row 22 px, pitch 23).

## Tests
JUnit `PassiveRulesTest` (percent, extra attempts, switch). Game tests: owner bonus, switch, farmland ignored, break forgets; Lv10 grows more often than Lv0.
