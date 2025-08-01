package com.elmakers.mine.bukkit.utility.platform.legacy;

import org.bukkit.entity.Entity;

import com.elmakers.mine.bukkit.utility.platform.Platform;
import com.elmakers.mine.bukkit.utility.platform.base.EntityUtilsBase;

public class EntityUtils extends EntityUtilsBase  {
    public EntityUtils(final Platform platform) {
        super(platform);
    }

    @Override
    public String getCustomName(Entity entity) {
        return null;
    }
}
