package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import bl4ckscor3.mod.scserverutils.SCServerUtils;
import net.minecraft.server.MinecraftServer;

/**
 * Changes the minecraft server's autosave interval from the default of five minutes
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@ModifyConstant(method = "computeNextAutosaveInterval", constant = @Constant(floatValue = 300.0F))
	private float reduceAutosaveInterval(float originalInterval) {
		return SCServerUtils.AUTOSAVE_INTERVAL;
	}
}
