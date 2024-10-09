package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bl4ckscor3.mod.scserverutils.SpawnProtectionHandler;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
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
	private void scserverutils$protectNether(ServerLevel level, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (level.dimension() == Level.NETHER) {
			if (!Configuration.instance.netherSpawnProtection.enabled().get() || getPlayerList().getOps().isEmpty() || getPlayerList().isOp(player.getGameProfile()))
				return;

			cir.setReturnValue(SpawnProtectionHandler.isInSpawnProtection(level, pos));
		}
	}
}
