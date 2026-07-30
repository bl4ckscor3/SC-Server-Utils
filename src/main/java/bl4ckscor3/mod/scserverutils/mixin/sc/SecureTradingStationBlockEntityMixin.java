package bl4ckscor3.mod.scserverutils.mixin.sc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.geforcemods.securitycraft.blockentities.SecureTradingStationBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

@Mixin(SecureTradingStationBlockEntity.class)
public class SecureTradingStationBlockEntityMixin {
	@Shadow
	public NonNullList<ItemStack> getContents() {
		throw new UnsupportedOperationException("Mixin implementation");
	}

	@Inject(method = "dropContents", at = @At("HEAD"))
	private void scserverutils$fixDupe(CallbackInfo ci) {
		for (int i = 0; i < 4; i++) {
			getContents().set(i, ItemStack.EMPTY);
		}
	}
}
