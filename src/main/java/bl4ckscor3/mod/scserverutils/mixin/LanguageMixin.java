package bl4ckscor3.mod.scserverutils.mixin;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.collect.ImmutableMap;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

/**
 * Adds the trigger kill death message to the language map
 */
@Mixin(Language.class)
public abstract class LanguageMixin {
	@Inject(method = "loadDefault", at = @At(value = "TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
	private static void securitycraft$injectCustom(CallbackInfoReturnable<Language> callback, ImmutableMap.Builder<String, String> builder, BiConsumer<String, String> biconsumer, Map<String, String> map) {
		callback.setReturnValue(new Language() {
			@Override
			public String getOrDefault(String p_128127_, String p_265421_) {
				if (p_128127_.equals("death.attack.trigger_kill"))
					return "%1$s found themselves in a dreadful situation and took the easy way out.";

				return map.getOrDefault(p_128127_, p_265421_);
			}

			@Override
			public boolean has(String p_128135_) {
				return map.containsKey(p_128135_);
			}

			@Override
			public boolean isDefaultRightToLeft() {
				return false;
			}

			@Override
			public FormattedCharSequence getVisualOrder(FormattedText text) {
				return (sink) -> {
					return text.visit((p_177835_, p_177836_) -> {
						return StringDecomposer.iterateFormatted(p_177836_, p_177835_, sink) ? Optional.empty() : FormattedText.STOP_ITERATION;
					}, Style.EMPTY).isPresent();
				};
			}

			@Override
			public Map<String, String> getLanguageData() {
				return map;
			}
		});
	}
}
