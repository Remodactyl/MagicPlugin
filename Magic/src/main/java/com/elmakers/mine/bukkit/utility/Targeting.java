package com.elmakers.mine.bukkit.utility;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageContext;
import com.elmakers.mine.bukkit.api.spell.TargetType;
import com.elmakers.mine.bukkit.magic.MagicMetaKeys;

public class Targeting {
    private static final Set<UUID> EMPTY_IGNORE_SET = Collections.emptySet();
    private static final Map<Entity, Hit>       projectileHits          = new WeakHashMap<>();

    private @Nonnull TargetingResult            result                  = TargetingResult.NONE;
    private Location                            source                  = null;

    private Target                              target                  = null;
    private List<Target>                        targets                 = null;

    private @Nonnull TargetType                 targetType              = TargetType.NONE;
    private BlockIterator                       blockIterator           = null;
    private Block                               currentBlock            = null;
    private Block                               previousBlock           = null;
    private Block                               previousPreviousBlock   = null;

    private Vector                              targetLocationOffset;
    private Vector                              targetDirectionOverride;
    private String                              targetLocationWorldName;

    protected float                             distanceWeight          = 1;
    protected float                             fovWeight               = 4;
    protected int                               npcWeight               = -1;
    protected int                               mageWeight              = 5;
    protected int                               playerWeight            = 4;
    protected int                               livingEntityWeight      = 3;

    private boolean                             ignoreBlocks            = false;
    private int                                 targetBreakableDepth    = 2;

    private double                              hitboxPadding           = 0;
    private double                              hitboxBlockPadding      = 0;
    private double                              rangeQueryPadding       = 1;
    private boolean                             useHitbox               = true;
    private double                              fov                     = 0.3;
    private double                              closeRange              = 0;
    private double                              closeFOV                = 0;
    private double                              yOffset                 = 0;
    private boolean                             targetSpaceRequired     = false;
    private int                                 targetMinOffset         = 0;
    private @Nonnull Set<UUID>                  ignoreEntities          = EMPTY_IGNORE_SET;

    public enum TargetingResult {
        NONE,
        BLOCK,
        ENTITY,
        MISS;

        public boolean isMiss() {
            return this == MISS || this == NONE;
        }
    }

    public void reset() {
        iterate();
        ignoreEntities = EMPTY_IGNORE_SET;
    }

    public void iterate() {
        result = TargetingResult.NONE;
        source = null;
        target = null;
        targets = null;
        blockIterator = null;
        currentBlock = null;
        previousBlock = null;
        previousPreviousBlock = null;
        targetSpaceRequired = false;
        targetMinOffset = 0;
        yOffset = 0;
    }

    protected boolean initializeBlockIterator(Location location, double range) {
        if (blockIterator != null) {
            return true;
        }

        try {
            blockIterator = new BlockIterator(location, yOffset, (int)Math.ceil(range));
        } catch (Exception ex) {
            if (Target.DEBUG_TARGETING) {
                org.bukkit.Bukkit.getLogger().warning("Exception creating BlockIterator");
                ex.printStackTrace();
            }
            // This seems to happen randomly, like when you use the same target.
            // Very annoying, and I now kind of regret switching to BlockIterator.
            // At any rate, we're going to just re-use the last target block and
            // cross our fingers!
            return false;
        }

        return true;
    }

    public Target getOrCreateTarget(Location defaultLocation) {
        if (target == null) {
            target = new Target(defaultLocation);
        }
        return target;
    }

    public Target getTarget() {
        return target;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public void setTargetSpaceRequired(boolean required) {
        targetSpaceRequired = required;
    }

    public void setTargetMinOffset(int offset) {
        targetMinOffset = offset;
    }

    public void targetBlock(Location source, Block block) {
        target = new Target(source, block, useHitbox, hitboxBlockPadding);
    }

    public void setYOffset(int offset) {
        yOffset = offset;
    }

    /**
     * Move "steps" forward along line of vision and returns the block there
     *
     * @return The block at the new location
     */
    @Nullable
    protected Block getNextBlock()
    {
        previousPreviousBlock = previousBlock;
        previousBlock = currentBlock;
        if (blockIterator == null || !blockIterator.hasNext()) {
            currentBlock = null;
        } else {
            currentBlock = blockIterator.next();
        }
        return currentBlock;
    }

    /**
     * Returns the current block along the line of vision
     *
     * @return The block
     */
    public Block getCurBlock()
    {
        return currentBlock;
    }

    /**
     * Returns the previous block along the line of vision
     *
     * @return The block
     */
    public Block getPreviousBlock()
    {
        return previousBlock;
    }

    public Block getPreviousPreviousBlock() {
        return previousPreviousBlock;
    }

    public void setFOV(double fov) {
        this.fov = fov;
    }

    public void setCloseRange(double closeRange) {
        this.closeRange = closeRange;
    }

    public void setCloseFOV(double closeFOV) {
        this.closeFOV = closeFOV;
    }

    public void setUseHitbox(boolean useHitbox) {
        this.useHitbox = useHitbox;
    }

    public TargetType getTargetType()
    {
        return targetType;
    }

    public void setTargetType(TargetType type) {
        targetType = type;
    }

    public void start(Location source) {
        iterate();
        this.source = source.clone();
    }

    public Target overrideTarget(MageContext context, Target target) {
        if (targetLocationOffset != null) {
            target.add(targetLocationOffset);
        }
        if (targetDirectionOverride != null) {
            target.setDirection(targetDirectionOverride);
        }
        if (targetLocationWorldName != null && targetLocationWorldName.length() > 0) {
            Location location = target.getLocation();
            if (location != null && context != null) {
                World targetWorld = location.getWorld();
                target.setWorld(ConfigurationUtils.overrideWorld(context.getController(), targetLocationWorldName, targetWorld, context.getController().canCreateWorlds()));
            }
        }
        this.target = target;
        return target;
    }

    public Target target(MageContext context, double range)
    {
        if (source == null)
        {
            source = context.getEyeLocation();
        }
        target = findTarget(context, range);
        target = overrideTarget(context, target);

        Mage mage = context.getMage();
        if (mage.getDebugLevel() > 15) {
            Location targetLocation = target.getLocation();
            String message = "";
            if (source != null) {
                message = message +  ChatColor.GREEN + "Targeted from " + ChatColor.GRAY + source.getBlockX()
                        + ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + source.getBlockY()
                        + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + source.getBlockZ()
                        + ChatColor.DARK_GREEN + " with range of " + ChatColor.GREEN + range + ChatColor.DARK_GREEN + ": "
                        + ChatColor.GOLD + result;
            }

            Entity targetEntity = target.getEntity();
            if (targetEntity != null) {
                message = message + ChatColor.DARK_GREEN + " (" + ChatColor.YELLOW + targetEntity.getType() + ChatColor.DARK_GREEN + ")";
            }
            if (targetLocation != null) {
                message = message + ChatColor.DARK_GREEN + " (" + ChatColor.LIGHT_PURPLE + targetLocation.getBlock().getType() + ChatColor.DARK_GREEN + ")";
                message = message + ChatColor.DARK_GREEN + " at "
                        + ChatColor.GRAY + targetLocation.getBlockX()
                        + ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + targetLocation.getBlockY()
                        + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + targetLocation.getBlockZ();
            }
            mage.sendDebugMessage(message);
        }

        return target;
    }

    /**
     * Returns the block at the cursor, or null if out of range
     *
     * @return The target block
     */
    protected Target findTarget(MageContext context, double range)
    {
        if (targetType == TargetType.NONE) {
            return new Target(source);
        }
        boolean isBlock = targetType == TargetType.BLOCK || targetType == TargetType.SELECT;

        Mage mage = context.getMage();
        final Entity mageEntity = mage.getEntity();
        if (targetType == TargetType.SELF && mageEntity != null) {
            result = TargetingResult.ENTITY;
            return new Target(source, mageEntity);
        }

        CommandSender sender = mage.getCommandSender();
        if (targetType == TargetType.SELF && mageEntity == null && sender != null && (sender instanceof BlockCommandSender)) {
            BlockCommandSender commandBlock = (BlockCommandSender)mage.getCommandSender();
            return new Target(commandBlock.getBlock().getLocation(), commandBlock.getBlock());
        }

        if (targetType == TargetType.SELF && source != null) {
            return new Target(source, source.getBlock());
        }

        if (targetType == TargetType.SELF) {
            return new Target(source);
        }

        Block block = null;
        if (!ignoreBlocks) {
            findTargetBlock(context, range);
            block = currentBlock;
        }

        Target targetBlock = null;
        if (block != null || isBlock) {
            if (result == TargetingResult.BLOCK) {
                targetBlock = new Target(source, block, useHitbox, hitboxBlockPadding);
            } else if (source != null) {
                Vector direction = source.getDirection();
                Location targetLocation = source.clone().add(direction.multiply(range));
                targetBlock = new Target(source, targetLocation, useHitbox, hitboxBlockPadding);
            }
        }

        if (isBlock) {
            return targetBlock;
        }

        // Don't target entities beyond the block we just hit,
        // but only if that block was solid, and not just at max range
        if (targetBlock != null && source != null && source.getWorld().equals(block.getWorld()) && !result.isMiss()) {
            range = Math.min(range, source.distance(targetBlock.getLocation()));
        }

        // Pick the closest candidate entity
        Target entityTarget = null;
        List<Target> scored = getAllTargetEntities(context, range);
        if (scored.size() > 0) {
            entityTarget = scored.get(0);
        }

        // Don't allow targeting entities in an area you couldn't cast the spell in
        if (context instanceof CastContext) {
            CastContext castContext = (CastContext)context;
            if (entityTarget != null && !castContext.canCast(entityTarget.getLocation())) {
                entityTarget = null;
            }
            if (targetBlock != null && !castContext.canCast(targetBlock.getLocation())) {
                result = TargetingResult.MISS;
                targetBlock = null;
            }
        }

        if (targetType == TargetType.OTHER_ENTITY && entityTarget == null) {
            result = TargetingResult.MISS;
            return new Target(source);
        }

        if (targetType == TargetType.ANY_ENTITY && entityTarget == null) {
            result = TargetingResult.ENTITY;
            return new Target(source, mageEntity);
        }

        if (entityTarget == null && targetType == TargetType.ANY && mageEntity != null) {
            result = TargetingResult.ENTITY;
            return new Target(source, mageEntity, targetBlock == null ? null : targetBlock.getBlock());
        }

        if (targetBlock != null && entityTarget != null) {
            if (targetBlock.getDistanceSquared() < entityTarget.getDistanceSquared() - hitboxPadding * hitboxPadding && !result.isMiss()) {
                entityTarget = null;
            } else {
                targetBlock = null;
            }
        }

        if (entityTarget != null) {
            result = TargetingResult.ENTITY;
            return entityTarget;
        } else if (targetBlock != null) {
            return targetBlock;
        }

        result = TargetingResult.MISS;
        return new Target(source);
    }

    protected void findTargetBlock(MageContext context, double range)
    {
        if (source == null || !CompatibilityLib.getCompatibilityUtils().isChunkLoaded(source))
        {
            return;
        }

        currentBlock = source.getBlock();
        if (isTargetable(context, currentBlock)) {
            result = TargetingResult.BLOCK;
            return;
        }

        // Pre-check for no block movement
        Location targetLocation = source.clone().add(source.getDirection().multiply(range));
        if (targetLocation.getBlockX() == source.getBlockX()
                && targetLocation.getBlockY() == source.getBlockY()
                && targetLocation.getBlockZ() == source.getBlockZ()) {

            result = TargetingResult.MISS;
            return;
        }

        if (!initializeBlockIterator(source, range))
        {
            return;
        }

        Block block = getNextBlock();
        result = TargetingResult.BLOCK;
        while (block != null)
        {
            if (targetMinOffset <= 0) {
                if (targetSpaceRequired && context instanceof CastContext) {
                    CastContext castContext = (CastContext)context;
                    if (!castContext.allowPassThrough(block)) {
                        break;
                    }
                    if (castContext.isOkToStandIn(block) && castContext.isOkToStandIn(block.getRelative(BlockFace.UP))) {
                        break;
                    }
                } else if (isTargetable(context, block)) {
                    break;
                }
            } else {
                targetMinOffset--;
            }
            block = getNextBlock();
        }
        if (block == null) {
            result = TargetingResult.MISS;
            currentBlock = previousBlock;
            previousBlock = previousPreviousBlock;
        }
    }

    private boolean isTargetable(MageContext context, Block block) {
        if (!context.isTargetable(block)) return false;
        if (useHitbox && !intersects(block)) return false;
        return true;
    }

    public boolean intersects(Block block) {
        Vector sourceDirection = source.getDirection();
        Vector sourceLocation = source.toVector();

        // Look out a long distance, enough to cover any valid range query
        Vector endPoint = sourceLocation.clone().add(sourceDirection.clone().multiply(1000));
        // Back up a bit
        Vector startPoint = sourceLocation.clone().add(sourceDirection.multiply(-0.1));

        Collection<BoundingBox> hitboxes = CompatibilityLib.getCompatibilityUtils().getBoundingBoxes(block);
        if (Target.DEBUG_TARGETING) {
            org.bukkit.Bukkit.getLogger().info(" Checking hitboxes for block "
                + block.getType() + " : " + hitboxes
                + " from " + startPoint.toBlockVector() + " to " + endPoint.toBlockVector()
                + " rot: " + sourceDirection);
        }
        for (BoundingBox hitbox : hitboxes) {
            if (hitboxPadding > 0) {
                hitbox.expand(hitboxPadding);
            }

            // This is a more efficient check as a first-pass
            if (hitbox.intersectsLine(startPoint, endPoint)) {
                if (Target.DEBUG_TARGETING) {
                    Vector hit = Target.getIntersection(block, startPoint, endPoint, hitboxPadding);
                    org.bukkit.Bukkit.getLogger().info(" Hit block at " + hit);
                }
                return true;
            }
        }
        return false;
    }

    public List<Target> getAllTargetEntities(MageContext context, double range) {
        Entity sourceEntity = context.getEntity();
        Mage mage = context.getMage();

        if (targets != null) {
            return targets;
        }
        targets = new ArrayList<>();

        // A fuzzy optimization range-check. A hard range limit is enforced in the final target consolidator
        double rangePadded = (range + hitboxPadding + rangeQueryPadding);
        rangePadded = Math.min(rangePadded, CompatibilityLib.getCompatibilityUtils().getMaxEntityRange());
        double rangeSquaredPadded = rangePadded * rangePadded;

        Collection<Entity> entities = null;
        boolean debugMessage = true;
        if (source == null && sourceEntity != null) {
            if (sourceEntity instanceof LivingEntity) {
                source = ((LivingEntity)sourceEntity).getEyeLocation();
            } else {
                source = sourceEntity.getLocation();
            }
        }
        if (source != null) {
            Vector queryRange = null;
            Location sourceLocation = source;
            if (useHitbox) {
                range = Math.min(range, CompatibilityLib.getCompatibilityUtils().getMaxEntityRange());
                Vector direction = source.getDirection();
                Location targetLocation = source.clone().add(direction.multiply(range));
                BoundingBox bounds = new BoundingBox(source.toVector(), targetLocation.toVector());
                bounds.expand(hitboxPadding + rangeQueryPadding);
                Vector center = bounds.center();
                sourceLocation = new Location(source.getWorld(), center.getX(), center.getY(), center.getZ());
                queryRange = bounds.size();
            } else {
                queryRange = new Vector(range * 2, range * 2, range * 2);
                sourceLocation = source;
            }

            entities = CompatibilityLib.getCompatibilityUtils().getNearbyEntities(sourceLocation, queryRange.getX() / 2, queryRange.getY() / 2, queryRange.getZ() / 2);

            if (targetType == TargetType.DISPLAY_ENTITY)
            {
                Collection<Entity> filteredEntities = entities;
                for (Entity entity : entities)
                {
                }
            }

            if (mage.getDebugLevel() > 16) {
                mage.sendDebugMessage(ChatColor.GREEN + "Targeting " + ChatColor.GOLD + entities.size() + ChatColor.GREEN + " entities from "
                        + ChatColor.GRAY + source.getBlockX()
                        + ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + source.getBlockY()
                        + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + source.getBlockZ()
                        + " via bounding box "
                        + ChatColor.GRAY + (int)Math.ceil(queryRange.getX())
                        + ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + (int)Math.ceil(queryRange.getY())
                        + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + (int)Math.ceil(queryRange.getZ())
                        + ChatColor.DARK_GREEN + " with range of " + ChatColor.GREEN + range);
                debugMessage = false;
            }
        }

        if (debugMessage && mage.getDebugLevel() > 17 && source != null) {
            mage.sendDebugMessage(ChatColor.GREEN + "Targeting entities from "
                    + ChatColor.GRAY + source.getBlockX()
                    + ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + source.getBlockY()
                    + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + source.getBlockZ()
                    + ChatColor.DARK_GREEN + " with range of " + ChatColor.GREEN + range);
        }

        if (entities == null) return targets;
        int useRange = (int)Math.ceil(range + hitboxPadding);
        for (Entity entity : entities)
        {
            if (ignoreEntities.contains(entity.getUniqueId())) continue;
            Location entityLocation = entity instanceof LivingEntity ? ((LivingEntity)entity).getEyeLocation() : entity.getLocation();
            if (!entityLocation.getWorld().equals(source.getWorld())) continue;
            if (entityLocation.distanceSquared(source) > rangeSquaredPadded) continue;
            if (!context.canTarget(entity)) continue;

            Target newScore = null;
            if (useHitbox) {
                newScore = new Target(source, entity, useRange, useHitbox, hitboxPadding);
            } else {
                newScore = new Target(source, entity, useRange, fov, closeRange, closeFOV,
                        distanceWeight, fovWeight, mageWeight, npcWeight, playerWeight, livingEntityWeight);
            }
            int requiredDebug = 20;
            if (newScore.getScore() > 0)
            {
                targets.add(newScore);
                requiredDebug = 11;
            }
            if (mage.getDebugLevel() > requiredDebug) {
                String message = ChatColor.DARK_GREEN + "Target "
                        + ChatColor.GREEN + entity.getType() + ChatColor.DARK_GREEN
                        + ": " + ChatColor.YELLOW + newScore.getScore()
                        + ChatColor.DARK_GREEN + ", r2: "
                        + ChatColor.GREEN + ((int)newScore.getDistanceSquared() + " / " + (useRange * useRange));
                if (!useHitbox) {
                    message += ChatColor.GREEN + ", a: " + newScore.getAngle();
                }
                mage.sendDebugMessage(message);
            }
        }

        Collections.sort(targets);
        return targets;
    }

    public void parseTargetType(String targetTypeName) {
        targetType = TargetType.NONE;
        if (targetTypeName != null) {
            try {
                //  Just a little convenience hack
                if (targetTypeName.equalsIgnoreCase("damager")) {
                    targetType = TargetType.LAST_DAMAGER;
                } else {
                    targetType = TargetType.valueOf(targetTypeName.toUpperCase());
                }
            } catch (Exception ex) {
                targetType = TargetType.NONE;
            }
        }
    }

    public void processParameters(ConfigurationSection parameters) {
        parseTargetType(parameters.getString("target"));
        useHitbox = parameters.getBoolean("hitbox", !parameters.contains("fov"));
        hitboxPadding = parameters.getDouble("hitbox_size", 0);
        hitboxBlockPadding = parameters.getDouble("hitbox_block_size", 0);
        rangeQueryPadding = parameters.getDouble("range_padding", 1);
        fov = parameters.getDouble("fov", 0.3);
        closeRange = parameters.getDouble("close_range", 1);
        closeFOV = parameters.getDouble("close_fov", 1.5);

        distanceWeight = (float)parameters.getDouble("distance_weight", 1);
        fovWeight = (float)parameters.getDouble("fov_weight", 4);
        npcWeight = parameters.getInt("npc_weight", -1);
        playerWeight = parameters.getInt("player_weight", 4);
        livingEntityWeight = parameters.getInt("entity_weight", 3);

        targetMinOffset = parameters.getInt("target_min_offset", 0);
        targetMinOffset = parameters.getInt("tmo", targetMinOffset);

        ignoreBlocks = parameters.getBoolean("ignore_blocks", false);
        targetBreakableDepth = parameters.getInt("target_breakable_depth", 2);

        targetLocationOffset = null;
        targetDirectionOverride = null;

        Double otxValue = ConfigurationUtils.getDouble(parameters, "otx", null);
        Double otyValue = ConfigurationUtils.getDouble(parameters, "oty", null);
        Double otzValue = ConfigurationUtils.getDouble(parameters, "otz", null);
        if (otxValue != null || otzValue != null || otyValue != null) {
            targetLocationOffset = new Vector(
                    (otxValue == null ? 0 : otxValue),
                    (otyValue == null ? 0 : otyValue),
                    (otzValue == null ? 0 : otzValue));
        }
        targetLocationWorldName = parameters.getString("otworld");

        Double tdxValue = ConfigurationUtils.getDouble(parameters, "otdx", null);
        Double tdyValue = ConfigurationUtils.getDouble(parameters, "otdy", null);
        Double tdzValue = ConfigurationUtils.getDouble(parameters, "otdz", null);
        if (tdxValue != null || tdzValue != null || tdyValue != null) {
            targetDirectionOverride = new Vector(
                    (tdxValue == null ? 0 : tdxValue),
                    (tdyValue == null ? 0 : tdyValue),
                    (tdzValue == null ? 0 : tdzValue));
        }
    }

    public TargetingResult getResult() {
        return result;
    }

    public void getTargetEntities(MageContext context, double range, int targetCount, Collection<WeakReference<Entity>> entities)
    {
        List<Target> candidates = getAllTargetEntities(context, range);
        if (targetCount < 0) {
            targetCount = candidates.size();
        }

        for (int i = 0; i < targetCount && i < candidates.size(); i++) {
            Target target = candidates.get(i);
            entities.add(new WeakReference<>(target.getEntity()));
        }
    }

    protected int breakBlockRecursively(MageContext mageContext, Block block, int depth) {
        if (depth <= 0 || !(mageContext instanceof CastContext)) return 0;
        CastContext context = (CastContext)mageContext;
        if (!context.isBreakable(block)) return 0;

        // Play break FX
        Location blockLocation = block.getLocation();
        Location effectLocation = blockLocation.add(0.5, 0.5, 0.5);
        context.playEffects("break", 1, context.getLocation(), null, effectLocation, null);

        // TODO: Re-examine this?
        UndoList undoList = com.elmakers.mine.bukkit.block.UndoList.getUndoList(blockLocation);
        if (undoList != null) {
            undoList.add(block);
        }

        context.clearBreakable(block);
        context.clearReflective(block);
        block.setType(Material.AIR);

        int broken = 1;
        if (depth > broken) {
            broken += breakBlockRecursively(context, block.getRelative(BlockFace.UP), Math.min(targetBreakableDepth, depth - broken));
            broken += breakBlockRecursively(context, block.getRelative(BlockFace.DOWN), Math.min(targetBreakableDepth, depth - broken));
            broken += breakBlockRecursively(context, block.getRelative(BlockFace.EAST), Math.min(targetBreakableDepth, depth - broken));
            broken += breakBlockRecursively(context, block.getRelative(BlockFace.WEST), Math.min(targetBreakableDepth, depth - broken));
            broken += breakBlockRecursively(context, block.getRelative(BlockFace.NORTH), Math.min(targetBreakableDepth, depth - broken));
            broken += breakBlockRecursively(context, block.getRelative(BlockFace.SOUTH), Math.min(targetBreakableDepth, depth - broken));
        }

        return broken;
    }

    public int breakBlock(CastContext context, Block block, double amount) {
        if (amount <= 0) return 0;
        Double breakableAmount = context.getBreakable(block);
        if (breakableAmount == null) return 0;

        double breakable = (int)(amount > 1 ? amount : (context.getRandom().nextDouble() < amount ? 1 : 0));
        if (breakable <= 0) return 0;
        return breakBlockRecursively(context, block, (int)Math.ceil(breakableAmount + breakable - 1));
    }

    public static void track(Entity tracked) {
        CompatibilityLib.getEntityMetadataUtils().setBoolean(tracked, MagicMetaKeys.TRACKING, true);
    }

    public static boolean checkTracking(Entity tracked, Entity target, Block block) {
        if (tracked == null || !CompatibilityLib.getEntityMetadataUtils().getBoolean(tracked, MagicMetaKeys.TRACKING)) {
            return false;
        }
        if (target != null) {
            projectileHits.put(tracked, new Hit(target));
        } else if (!tracked.hasMetadata("hit")) {
            // Don't overwrite entity hits, projectiles that explode seem to send multiple ProjectIleHit events
            // which will stomp on the EDBE event that tracks
            Hit current = projectileHits.get(tracked);
            if (current == null || current.getEntity() == null) {
                projectileHits.put(tracked, new Hit(block));
            }
        }

        return true;
    }

    public static Hit getHit(Entity tracked) {
        return projectileHits.get(tracked);
    }

    public void ignoreEntity(Entity entity) {
        if (ignoreEntities == EMPTY_IGNORE_SET) {
            ignoreEntities = new HashSet<>();
        }
        ignoreEntities.add(entity.getUniqueId());
    }
}
