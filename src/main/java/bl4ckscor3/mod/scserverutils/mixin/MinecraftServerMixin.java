package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.server.MinecraftServer;

/**
 * Changes the minecraft server's autosave interval from the default of five minutes
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@ModifyConstant(method = "computeNextAutosaveInterval", constant = @Constant(floatValue = 300.0F))
	private float scserverutils$reduceAutosaveInterval(float originalInterval) {
		return Configuration.instance.autosaveInterval.interval().get();
	}
}
