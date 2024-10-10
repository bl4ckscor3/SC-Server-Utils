package bl4ckscor3.mod.scserverutils.mixin;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import bl4ckscor3.mod.scserverutils.SpawnProtectionHandler;
import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import bl4ckscor3.mod.scserverutils.configuration.SuppressDestroyMismatchLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerLevel level;
	@Shadow
	private BlockPos destroyPos;

	@WrapWithCondition(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
	private boolean scserverutils$logBlockBreakMismatch(Logger logger, String message, Object destroyPos, Object pos) {
		SuppressDestroyMismatchLog suppressDestroyMismatchLog = Configuration.instance.suppressDestroyMismatchLog;

		if (!suppressDestroyMismatchLog.enabled().get())
			return true;

		return suppressDestroyMismatchLog.onlyInSpawnProtection().get() && !SpawnProtectionHandler.isInSpawnProtection(level, this.destroyPos);
	}
}
