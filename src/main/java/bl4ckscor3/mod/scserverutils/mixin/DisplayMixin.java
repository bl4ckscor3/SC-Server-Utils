package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import bl4ckscor3.mod.scserverutils.configuration.Configuration;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;

@Mixin(value = {
		Display.class, Display.ItemDisplay.class, Display.TextDisplay.class
})
public class DisplayMixin {
	@ModifyConstant(method = "readAdditionalSaveData", constant = @Constant(stringValue = "Display entity"))
	private String scserverutils$verboseDisplayEntityLogging(String original) {
		if (Configuration.instance.verboseDisplayEntityLogging.enabled().get()) {
			Entity self = (Entity) (Object) this;

			return original + " " + self.getStringUUID() + " at " + self.position() + " errored: ";
		}
		else
			return original;
	}
}
