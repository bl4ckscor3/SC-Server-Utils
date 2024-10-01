package bl4ckscor3.mod.scserverutils.mixin;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.server.level.ServerPlayerGameMode;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@WrapWithCondition(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
	private boolean scserverutils$silenceBlockBreakMismatch(Logger logger, String message, Object destroyPos, Object pos) {
		return !Configuration.instance.destroyMismatchLogFix.enabled().get();
	}
}
