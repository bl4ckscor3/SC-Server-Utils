package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public record DamageSourceLanguageFallback(BooleanValue enabled) implements MixinModifier {
	@Override
	public boolean hasMixinClass(String mixinClassName) {
		return mixinClassName.equals("DamageSourceMixin");
	}
}