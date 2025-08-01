package com.elmakers.mine.bukkit.api.spell;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.block.MaterialAndData;
import com.elmakers.mine.bukkit.api.block.MaterialBrush;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.VariableScope;
import com.elmakers.mine.bukkit.api.wand.Wand;

/**
 * Represents a Spell that may be cast by a Mage.
 *
 * <p>Each Spell is based on a SpellTemplate, which are defined
 * by the spells configuration files.
 *
 * <p>Every spell uses a specific Class that must extend from
 * com.elmakers.mine.bukkit.plugins.magic.spell.Spell.
 *
 * <p>To create a new custom spell from scratch, you must also
 * implement the MageSpell interface.
 */
public interface Spell extends SpellTemplate {
    MageController getController();
    boolean cast();
    boolean cast(@Nullable String[] parameters);
    boolean cast(@Nullable String[] parameters, @Nullable Location defaultLocation);
    boolean cast(@Nullable ConfigurationSection parameters, @Nullable Location defaultLocation);
    boolean cast(@Nullable ConfigurationSection parameters);
    boolean cast(@Nullable Wand wand, @Nullable ConfigurationSection parameters);
    @Nullable
    Location getLocation();
    Entity getEntity();
    Location getEyeLocation();
    void target();
    @Nullable
    Location getTargetLocation();
    @Nullable
    Entity getTargetEntity();
    Vector getDirection();
    boolean canTarget(Entity entity);
    boolean isActive();
    boolean hasBrushOverride();
    boolean canContinue(Location location);
    boolean canCast(Location location);
    void clearCooldown();
    void reduceRemainingCooldown(long ms);
    void setRemainingCooldown(long ms);
    long getRemainingCooldown();
    @Nullable
    CastingCost getRequiredCost();
    void messageTargets(String messageKey);
    CastContext getCurrentCast();
    void playEffects(String effectName);
    void playEffects(String effectName, CastContext context);
    void playEffects(String effectName, CastContext context, float scale);
    boolean requiresBuildPermission();
    boolean requiresBreakPermission();
    boolean isPvpRestricted();
    boolean isDisguiseRestricted();
    void sendMessage(String message);
    void castMessage(String message);
    void sendMessageKey(String key, String message);
    void castMessageKey(String key, String message);
    MaterialAndData getEffectMaterial();
    @Nullable
    String getEffectParticle();
    @Nullable
    Color getEffectColor();
    @Nullable
    MaterialBrush getBrush();
    boolean brushIsErase();
    boolean isCancellable();
    ConfigurationSection getWorkingParameters();
    void finish(CastContext context);
    double cancelOnDamage();
    boolean cancelOnWorldChange();
    boolean cancelOnEnterPortal();
    boolean cancelOnCastOther();
    boolean cancelOnDeath();
    boolean cancelOnDeactivate();
    String getMessage(String messageKey);
    boolean hasHandlerParameters(String handlerKey);
    @Nullable
    ConfigurationSection getHandlerParameters(String handlerKey);
    long getProgressLevel();
    boolean cancelOnNoPermission();
    boolean cancelOnNoWand();
    boolean bypassesDeactivate();
    boolean reactivate();
    @Nonnull
    ConfigurationSection getVariables();
    default void reloadParameters(CastContext context) {
    }

    /**
     * Signal that this spell was cancelled. Will send cancel messages
     * and play cancel FX.
     *
     * <p>This will not actually cancel any pending spell casts (batches) of this spell,
     * for that you will need Mage.cancelPending
     *
     * @return true (for legacy reasons)
     */
    boolean cancel();

    /**
     * Like a quiet version of cancel() - at some point cancel, stop and deactivate should all get cleaned up.
     */
    boolean stop();

    /**
     * Cancel a selection in-progress for a two-click selection spell (like Architect magic)
     *
     * <p>Will call cancel() if selection was cancelled.
     *
     * @return true if the spell was in the middle of selection and was cancelled.
     */
    boolean cancelSelection();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    long getLastCast();
    double getChargesRemaining();
    @Nullable
    VariableScope getVariableScope(String variableName);

    /**
     * This is used to control visibility of a spell's particle effects (and audibility of its sound effects)
     * to a specific list of players.
     *
     * <p>When set, this will override the "visibility" and "broadcast" configurations of this spell.
     *
     * <p>This change will stick on the spell until reboot. Use #clearObservers to reset to the default
     * behavior.
     *
     * @param players The list of players who can see and hear this spell
     */
    default void setObservers(@Nonnull Collection<Player> players) {
        // This default is here to not break any plugins implementing custom spells from scratch
        throw new UnsupportedOperationException("Custom spell visibility is not implemented");
    }

    default void setObserverIds(@Nonnull List<UUID> players) {
        // This default is here to not break any plugins implementing custom spells from scratch
        throw new UnsupportedOperationException("Custom spell visibility is not implemented");
    }

    /**
     * Reset custom visibility (via #setObservers) to the default.
     */
    default void clearObservers() {
        // This default is here to not break any plugins implementing custom spells from scratch
        throw new UnsupportedOperationException("Custom spell visibility is not implemented");
    }
}
