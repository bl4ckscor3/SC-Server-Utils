package bl4ckscor3.mod.scserverutils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Sends the death message for /trigger kill_self with a fallback message, so players without the server resource pack still
 * get the death message shown properly
 */
@Mixin(DamageSource.class)
public abstract class DamageSourceMixin {
	@WrapOperation(method = "getLocalizedDeathMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
	private MutableComponent addFallbackToTriggerKillMessage(String fullMsgId, Object[] args, Operation<MutableComponent> original) {
		MutableComponent originalReturnValue = original.call(fullMsgId, args);

		if (fullMsgId.equals("death.attack.trigger_kill"))
			return Component.translatableWithFallback(fullMsgId, originalReturnValue.getString(), args);
		else
			return originalReturnValue;
	}
}
