package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@Inject(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), cancellable = true)
	private void scserverutils$disableSnowInSpawnProtection(BlockPos pos, CallbackInfo ci) {
		if (Configuration.instance.noSpawnProtectionSnow.enabled().get()) {
			ServerLevel level = (ServerLevel) (Object) this;
			BlockPos spawnPos = level.getSharedSpawnPos();
			int adjustedX = Mth.abs(pos.getX() - spawnPos.getX());
			int adjustedZ = Mth.abs(pos.getZ() - spawnPos.getZ());

			if (Math.max(adjustedX, adjustedZ) <= level.getServer().getSpawnProtectionRadius())
				ci.cancel();
		}
	}
}
