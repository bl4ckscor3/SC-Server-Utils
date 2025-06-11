package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;

@Mixin(PlayerRespawnLogic.class)
public class PlayerRespawnLogicMixin {
	@ModifyReturnValue(method = "getOverworldRespawnPos", at = @At(value = "RETURN", ordinal = 2))
	private static BlockPos scserverutils$respectY(BlockPos original, ServerLevel level, int x, int z) {
		if (Configuration.instance.spawnProtection.spawnLocationY().get()) {
			BlockPos spawnPos = level.getSharedSpawnPos();

			if (spawnPos.getX() == x && spawnPos.getZ() == z)
				return spawnPos.above().immutable();
		}

		return original;
	}
}
