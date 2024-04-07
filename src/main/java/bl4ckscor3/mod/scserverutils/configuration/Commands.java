package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public record Commands(BooleanValue enderchestEnabled, BooleanValue invseeEnabled, BooleanValue rulesEnabled) {
	public static BooleanValue createConfig(ModConfigSpec.Builder builder, String commandName) {
		return builder.define(commandName + "_enabled", true);
	}
}