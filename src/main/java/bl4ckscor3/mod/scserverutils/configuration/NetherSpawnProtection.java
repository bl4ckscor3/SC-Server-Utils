package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record NetherSpawnProtection(BooleanValue enabled, IntValue radius, IntValue xOrigin, IntValue zOrigin) implements MixinModifier {
	@Override
	public boolean hasMixinClass(String mixinClassName) {
		return mixinClassName.equals("DedicatedServerMixin");
	}
}
