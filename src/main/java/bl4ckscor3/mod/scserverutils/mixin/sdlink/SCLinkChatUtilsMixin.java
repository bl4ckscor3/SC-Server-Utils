package bl4ckscor3.mod.scserverutils.mixin.sdlink;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.hypherionmc.sdlink.util.SDLinkChatUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(SDLinkChatUtils.class)
public class SCLinkChatUtilsMixin {
	@ModifyReturnValue(method = "applyFiltering", at = @At("TAIL"))
	private static String scserverutils$filterParagraphSign(String original) {
		return original.replace("$", "");
	}
}
