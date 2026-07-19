package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import bl4ckscor3.mod.scserverutils.SpawnProtectionHandler;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.BlockBypass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@WrapOperation(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isUnderSpawnProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
	private boolean scserverutils$notInSpawnProtectionIfInteractionWithAllowedBlock(MinecraftServer server, ServerLevel level, BlockPos pos, Player player, Operation<Boolean> original) {
		return !scserverutils$interactsWithAllowedBlock(level, pos) && original.call(server, level, pos, player);
	}

	@WrapOperation(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
	private boolean scserverutils$allowInteractionInSpawnProtection(ServerLevel level, Entity entity, BlockPos pos, Operation<Boolean> original) {
		return scserverutils$interactsWithAllowedBlock(level, pos) || original.call(level, entity, pos);
	}

	@Unique
	private static boolean scserverutils$interactsWithAllowedBlock(ServerLevel level, BlockPos pos) {
		BlockBypass spawnProtectionBlockBypass = Configuration.instance.spawnProtection.blockBypass();

		if (spawnProtectionBlockBypass.enabled().get() && SpawnProtectionHandler.isInSpawnProtection(level, pos)) {
			return level.getBlockState(pos).is(BlockBypass.CAN_INTERACT_WITH_IN_SPAWN_PROTECTION);
		}

		return false;
	}
}
