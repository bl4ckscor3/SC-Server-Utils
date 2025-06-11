package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import bl4ckscor3.mod.scserverutils.SpawnProtectionHandler;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.BlockBypass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
	private boolean scserverutils$allowInteractionInSpawnProtection(ServerLevel level, Entity entity, BlockPos pos) {
		BlockBypass spawnProtectionBlockBypass = Configuration.instance.spawnProtection.blockBypass();

		if (spawnProtectionBlockBypass.enabled().get() && SpawnProtectionHandler.isInSpawnProtection(level, pos)) {
			String blockId = level.getBlockState(pos).getBlockHolder().getRegisteredName();

			if (spawnProtectionBlockBypass.blocks().get().contains(blockId))
				return true;
		}

		return level.mayInteract(entity, pos);
	}
}
