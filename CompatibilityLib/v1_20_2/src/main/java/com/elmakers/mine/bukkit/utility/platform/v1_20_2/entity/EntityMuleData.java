package com.elmakers.mine.bukkit.utility.platform.v1_20_2.entity;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import com.elmakers.mine.bukkit.api.magic.MageController;

public class EntityMuleData extends EntityChestedHorseData {
    public EntityMuleData(ConfigurationSection parameters, MageController controller) {
        super(parameters, controller);
    }

    public EntityMuleData(Entity entity, MageController controller) {
        super(entity, controller);
    }
}
