package bl4ckscor3.mod.scserverutils.mixin;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * Removes the "Gathered mod list to write to world save world" message, as it gets quite spammy with the changed autosave
 * interval
 */
@Mixin(CommonHooks.class)
public class CommonHooksMixin {
	@WrapWithCondition(method = "writeAdditionalLevelSaveData", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;debug(Lorg/apache/logging/log4j/Marker;Ljava/lang/String;Ljava/lang/Object;)V"))
	private static boolean removeDebugOutput(Logger logger, Marker marker, String line, Object arg) {
		return !Configuration.instance.autosaveInterval.removeNeoForgeLogMessage().get();
	}
}
