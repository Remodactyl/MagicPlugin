package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.utility.CompatibilityLib;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.effect.SoundEffect;

public class StopSoundAction extends BaseSpellAction
{
    private SoundEffect sound;
    private boolean all = false;

    @Override
    public SpellResult perform(CastContext context)
    {
        if (sound == null) {
            return SpellResult.FAIL;
        }
        Entity target = context.getTargetEntity();
        if (!(target instanceof Player)) {
            return SpellResult.PLAYER_REQUIRED;
        }

        Player player = (Player)target;
        if(all) {
            CompatibilityLib.getCompatibilityUtils().stopSound(player);
            return SpellResult.CAST;
        }

        sound.stop(player);
        return SpellResult.CAST;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public boolean requiresTargetEntity() {
        return true;
    }

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        sound = new SoundEffect(parameters.getString("sound"));
        all = parameters.getBoolean("all", false);
    }
}
