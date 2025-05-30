package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.MasteryUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;
import shipmastery.util.VariantLookup;

import java.util.Objects;

public class MasteryHullmod extends BaseHullMod {

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats,
                                               final String id) {
        ShipVariantAPI variant = stats.getVariant();
        // Add an S-mod slot if the logistics enhance bonus is active and the ship has at least one logistics hullmod
        if (shouldApplyEffects(variant)) {
            if (SModUtils.hasBonusLogisticSlot(variant) && SModUtils.hasLogisticSMod(variant)) {
                stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, 1);
            } else {
                stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).unmodify(id);
            }
        }

        applyEffects(variant, (effect, commander, isModule) -> {
            if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                effect.applyEffectsBeforeShipCreation(hullSize, stats);
                // For display purposes only
                FleetMemberAPI fm = stats.getFleetMember();
                final PersonAPI captain = fm == null ? null : fm.getCaptain();
                if (commander != null && Objects.equals(commander, captain)) {
                    effect.onFlagshipStatusGained(commander, stats, null);
                }
            }
        });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        applyEffects(ship.getVariant(), (effect, commander, isModule) -> {
            if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                effect.applyEffectsAfterShipCreation(ship);
                // For display purposes only
                final PersonAPI captain = ship.getCaptain();
                if (commander != null && Objects.equals(commander, captain)) {
                    effect.onFlagshipStatusGained(commander, ship.getMutableStats(), null);
                }
            }
        });
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        applyEffects(ship.getVariant(), (effect, commander, isModule) -> {
            if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                effect.applyEffectsToFighterSpawnedByShip(fighter, ship);
            }
        });
    }

    // Extra safety against recursive calls not handled by forcing no-sync for fleet, i.e. in variant.updateStatsForOpCosts, etc.
    boolean noRecurse = false;
    private void applyEffects(final ShipVariantAPI variant, final HullmodAction action) {
        if (variant == null || noRecurse || !shouldApplyEffects(variant)) {
            return;
        }

        final VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(variant);
        final ShipHullSpecAPI rootSpec = info == null ? variant.getHullSpec() : info.root.getHullSpec();
        final boolean isModule = info != null && !Objects.equals(info.uid, info.rootUid);
        final PersonAPI commander = info == null ? null : info.commander;
        CampaignFleetAPI fleet = info == null ? null : info.fleet;
        noRecurse = true;
        // Needed because getting masteries calls getFlagship, which updates stats, which calls applyEffectsBeforeShipCreation, etc.
        boolean wasNoSync = false;
        if (fleet != null) {
            wasNoSync = fleet.getFleetData().isForceNoSync();
            fleet.getFleetData().setForceNoSync(true);
        }
        MasteryUtils.applyAllActiveMasteryEffects(
                commander, rootSpec, effect -> action.perform(effect, commander, isModule));
        if (fleet != null) {
            fleet.getFleetData().setForceNoSync(wasNoSync);
        }
        noRecurse = false;
    }

    private boolean shouldApplyEffects(ShipVariantAPI variant) {
        return !Settings.DISABLE_MAIN_FEATURES && variant != null && !variant.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE);
    }

    private interface HullmodAction {
        void perform(MasteryEffect effect, PersonAPI commander, boolean isModule);
    }
}
