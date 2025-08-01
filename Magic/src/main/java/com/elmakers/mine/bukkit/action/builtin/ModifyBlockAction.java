package com.elmakers.mine.bukkit.action.builtin;

import java.util.Arrays;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.block.MaterialBrush;
import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.block.DefaultMaterials;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import com.elmakers.mine.bukkit.utility.CompatibilityLib;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.utility.SafetyUtils;

public class ModifyBlockAction extends BaseSpellAction {
    private boolean spawnFallingBlocks;
    private boolean fallingBlocksHurt;
    private double fallingBlockSpeed;
    private Vector fallingBlockDirection;
    private float fallingBlockFallDamage;
    private int fallingBlockMaxDamage;
    private double fallingProbability;
    private double breakable = 0;
    private double backfireChance = 0;
    private boolean applyPhysics = false;
    private boolean exactLocation = false;
    private int x, y, z;
    private boolean commit = false;
    private boolean consumeBlocks = false;
    private boolean consumeVariants = true;
    private boolean checkChunk = false;
    private boolean autoBlockState = false;
    private boolean replaceSame = false;

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        spawnFallingBlocks = parameters.getBoolean("falling", false);
        applyPhysics = parameters.getBoolean("physics", false);
        autoBlockState = parameters.getBoolean("auto_block_state", false);
        commit = parameters.getBoolean("commit", false);
        breakable = parameters.getDouble("breakable", 0);
        backfireChance = parameters.getDouble("reflect_chance", 0);
        fallingBlockSpeed = parameters.getDouble("speed", 0);
        fallingProbability = parameters.getDouble("falling_probability", 1);
        consumeBlocks = parameters.getBoolean("consume", false);
        consumeVariants = parameters.getBoolean("consume_variants", true);
        fallingBlocksHurt = parameters.getBoolean("falling_hurts", false);
        checkChunk = parameters.getBoolean("check_chunk", true);

        if(parameters.contains("x")) {
            x = parameters.getInt("x");
            y = parameters.getInt("y");
            z = parameters.getInt("z");
            exactLocation = true;
        }

        replaceSame = parameters.getBoolean("replace_same", false);
        fallingBlockDirection = null;
        if (spawnFallingBlocks && parameters.contains("direction") && !parameters.getString("direction").isEmpty())
        {
            if (fallingBlockSpeed == 0) {
                fallingBlockSpeed = 1;
            }
            fallingBlockDirection = ConfigurationUtils.getVector(parameters, "direction");
        }

        int damage = parameters.getInt("damage", 0);
        fallingBlockFallDamage = (float)parameters.getDouble("fall_damage", damage);
        fallingBlockMaxDamage = parameters.getInt("max_damage", damage);
    }

    @SuppressWarnings("deprecation")
    @Override
    public SpellResult perform(CastContext context) {
        MaterialBrush brush = context.getBrush();
        if (brush == null) {
            return SpellResult.FAIL;
        }

        if (checkChunk && !CompatibilityLib.getCompatibilityUtils().checkChunk(context.getTargetLocation())) {
            context.addWork(100);
            return SpellResult.PENDING;
        }

        Block block = context.getTargetBlock();
        if(exactLocation) {
            Location newBlockLocation = new Location(block.getWorld(), x, y, z);
            block = newBlockLocation.getBlock();
        }

        if (brush.isErase()) {
            if (!context.hasBreakPermission(block)) {
                return SpellResult.INSUFFICIENT_PERMISSION;
            }
        } else {
            if (!context.hasBuildPermission(block)) {
                return SpellResult.INSUFFICIENT_PERMISSION;
            }
        }

        if (commit)
        {
            if (!context.areAnyDestructible(block)) {
                return SpellResult.NO_TARGET;
            }
        }
        else if (!context.isDestructible(block)) {
            return SpellResult.NO_TARGET;
        }

        Material fallingMaterial = block.getType();
        String fallingData = CompatibilityLib.getCompatibilityUtils().getBlockData(block);
        byte fallingLegacyData = block.getData();

        Mage mage = context.getMage();
        brush.update(mage, context.getTargetSourceLocation());

        if (!brush.isReady()) {
            brush.prepare();
            return SpellResult.PENDING;
        }

        if (!brush.isValid()) {
            return SpellResult.FAIL;
        }
        if (!brush.isTargetValid()) {
            return SpellResult.NO_TARGET;
        }

        if (!replaceSame && !brush.isDifferent(block)) {
            return SpellResult.NO_TARGET;
        }

        if (consumeBlocks && !context.isConsumeFree() && !brush.isErase()) {
            UndoList undoList = context.getUndoList();
            if (undoList != null) {
                undoList.setConsumed(true);
            }
            if (!mage.consumeBlock(brush, consumeVariants)) {
                String requiresMessage = context.getMessage("insufficient_resources");
                context.sendMessageKey("insufficient_resources", requiresMessage.replace("$cost", brush.getName()));
                return SpellResult.INSUFFICIENT_RESOURCES;
            }
        }

        boolean spawnFalling = spawnFallingBlocks;
        if (spawnFalling && fallingProbability < 1) {
            spawnFalling = context.getRandom().nextDouble() < fallingProbability;
        }

        if (spawnFalling && !brush.isErase()) {
            fallingMaterial = brush.getMaterial();
            fallingData = brush.getModernBlockData();
            Byte data = brush.getBlockData();
            fallingLegacyData = data == null ? 0 : data;
        } else {
            if (!commit) {
                context.registerForUndo(block);
                if (brush.isErase() && !DefaultMaterials.isAir(block.getType())) {
                    context.clearAttachables(block);
                }
            }
            UndoList undoList = context.getUndoList();
            if (undoList != null) {
                undoList.setApplyPhysics(applyPhysics);
            }
            BlockState prior = block.getState();
            brush.modify(block, applyPhysics);
            if (undoList != null && !undoList.isScheduled()) {
                context.getController().logBlockChange(context.getMage(), prior, block.getState());
            }

            if (autoBlockState) {
                Location targetLocation = context.getTargetLocation();
                Block hitBlock = targetLocation.getBlock();
                BlockFace direction = hitBlock.getFace(block);
                if (direction == BlockFace.SELF) {
                    direction = BlockFace.UP;
                }
                CompatibilityLib.getCompatibilityUtils().setAutoBlockState(block, targetLocation, direction, applyPhysics, context.getMage().getPlayer());
                /*
                BlockFace[] neighbors = {BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN};
                for (BlockFace blockFace : neighbors) {
                    Block neighbor = block.getRelative(blockFace);
                    CompatibilityUtils.forceUpdate(neighbor, applyPhysics);
                }
                */
            }
        }
        spawnFalling = spawnFalling && !DefaultMaterials.isAir(fallingMaterial);
        if (spawnFalling)
        {
            Location blockLocation = block.getLocation();
            Location blockCenter = new Location(blockLocation.getWorld(), blockLocation.getX() + 0.5, blockLocation.getY() + 0.5, blockLocation.getZ() + 0.5);
            Vector fallingBlockVelocity = null;
            if (fallingBlockSpeed > 0) {
                Location source = context.getTargetCenterLocation();
                fallingBlockVelocity = blockCenter.clone().subtract(source).toVector();
                fallingBlockVelocity.normalize();

                if (fallingBlockDirection != null)
                {
                    fallingBlockVelocity.add(fallingBlockDirection).normalize();
                }
                fallingBlockVelocity.multiply(fallingBlockSpeed);
            }
            if (fallingBlockVelocity != null && (
                   Double.isNaN(fallingBlockVelocity.getX()) || Double.isNaN(fallingBlockVelocity.getY()) || Double.isNaN(fallingBlockVelocity.getZ())
                || Double.isInfinite(fallingBlockVelocity.getX()) || Double.isInfinite(fallingBlockVelocity.getY()) || Double.isInfinite(fallingBlockVelocity.getZ())
            ))
            {
                fallingBlockVelocity = null;
            }

            // If not using erase, spawn falling block instead of placing a block
            FallingBlock falling;
            if (fallingData != null) {
                falling = CompatibilityLib.getCompatibilityUtils().spawnFallingBlock(blockCenter, fallingMaterial, fallingData);
            } else {
                falling = CompatibilityLib.getDeprecatedUtils().spawnFallingBlock(blockCenter, fallingMaterial, fallingLegacyData);
            }
            falling.setDropItem(false);
            if (fallingBlockVelocity != null) {
                SafetyUtils.setVelocity(falling, fallingBlockVelocity);
            }
            if (fallingBlockMaxDamage > 0 && fallingBlockFallDamage > 0) {
                CompatibilityLib.getCompatibilityUtils().setFallingBlockDamage(falling, fallingBlockFallDamage, fallingBlockMaxDamage);
            } else {
                falling.setHurtEntities(fallingBlocksHurt);
            }
            context.registerForUndo(falling);
        }

        if (breakable > 0) {
            context.registerBreakable(block, breakable);
        }
        if (backfireChance > 0) {
            context.registerReflective(block, backfireChance);
        }

        if (commit) {

            com.elmakers.mine.bukkit.api.block.BlockData blockData = context.getUndoList().get(block);;
            blockData.commit();
        }
        return SpellResult.CAST;
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters) {
        super.getParameterNames(spell, parameters);
        parameters.add("falling");
        parameters.add("speed");
        parameters.add("direction");
        parameters.add("reflect_chance");
        parameters.add("breakable");
        parameters.add("physics");
        parameters.add("commit");
        parameters.add("hurts");
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples) {
        if (parameterKey.equals("falling") || parameterKey.equals("physics") || parameterKey.equals("commit") || parameterKey.equals("falling_hurts")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_BOOLEANS));
        } else if (parameterKey.equals("speed") || parameterKey.equals("breakable")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_SIZES));
        } else if (parameterKey.equals("direction")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_VECTOR_COMPONENTS));
        } else if (parameterKey.equals("reflect_chance")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_PERCENTAGES));
        } else {
            super.getParameterOptions(spell, parameterKey, examples);
        }
    }

    @Override
    public boolean requiresBuildPermission() {
        return true;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public boolean isUndoable() {
        return true;
    }

    @Override
    public boolean usesBrush() {
        return true;
    }
}
