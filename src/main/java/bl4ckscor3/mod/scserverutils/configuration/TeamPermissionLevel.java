package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record TeamPermissionLevel(BooleanValue enabled, IntValue permissionLevel) implements MixinModifier {
	@Override
	public boolean hasMixinClass(String mixinClassName) {
		return mixinClassName.equals("TeamCommandMixin");
	}
}