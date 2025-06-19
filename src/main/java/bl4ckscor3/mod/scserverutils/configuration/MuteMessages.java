package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public record MuteMessages(ModConfigSpec.ConfigValue<String> enter, ModConfigSpec.ConfigValue<String> leave) {}
