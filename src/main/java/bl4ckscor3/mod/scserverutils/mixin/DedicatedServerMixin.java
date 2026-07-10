package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bl4ckscor3.mod.scserverutils.SpawnProtectionHandler;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.spawnprotection.Dimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
	@Shadow
	public abstract DedicatedPlayerList getPlayerList();

	@Inject(method = "isUnderSpawnProtection", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
	private void scserverutils$protectDimensions(ServerLevel level, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (level.dimension() == Level.NETHER)
			scserverutils$protectSpecificDimension(Configuration.instance.spawnProtection.nether(), level, pos, player, cir);
		else if (level.dimension() == Level.END)
			scserverutils$protectSpecificDimension(Configuration.instance.spawnProtection.end(), level, pos, player, cir);
	}

	@Unique
	private void scserverutils$protectSpecificDimension(Dimension dimensionConfig, ServerLevel level, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!dimensionConfig.enabled().get() || getPlayerList().getOps().isEmpty() || getPlayerList().isOp(player.nameAndId()))
			return;

		cir.setReturnValue(!player.entityTags().contains(dimensionConfig.bypassTag().get()) && SpawnProtectionHandler.isInSpawnProtection(level, pos));
	}
}
