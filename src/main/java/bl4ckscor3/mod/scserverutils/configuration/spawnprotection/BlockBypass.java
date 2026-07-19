package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public record BlockBypass(BooleanValue enabled) {
	public static final TagKey<Block> CAN_INTERACT_WITH_IN_SPAWN_PROTECTION = TagKey.create(Registries.BLOCK, SCServerUtils.id("can_interact_with_in_spawn_protection"));
}