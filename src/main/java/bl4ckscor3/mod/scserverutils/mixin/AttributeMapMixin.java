package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/**
 * Gets rid of "Unknown attribute" spam, because there is a lot of that when loading chunks in NeoForge that have last been
 * saved with Forge. It also prepares for the removal of certain attributes that are in vanilla since 1.20.5
 */
@Mixin(AttributeMap.class)
public class AttributeMapMixin {
	@ModifyArg(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;ifElse(Ljava/util/Optional;Ljava/util/function/Consumer;Ljava/lang/Runnable;)Ljava/util/Optional;"), index = 2)
	public Runnable scserverutils$silenceUnknownAttributeLine(Runnable oldOrElse) {
		if (Configuration.instance.attributeLogFix.enabled().get())
			return () -> {};
		else
			return oldOrElse;
	}
}
