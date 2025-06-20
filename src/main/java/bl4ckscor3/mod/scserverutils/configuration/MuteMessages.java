package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public record MuteMessages(ModConfigSpec.ConfigValue<String> cancelledMessageMute, 
ModConfigSpec.ConfigValue<String> cancelledMessageSuspend, 
ModConfigSpec.ConfigValue<String> muteStarts,
ModConfigSpec.ConfigValue<String> muteEnds,
ModConfigSpec.ConfigValue<String> muteFailedAlreadyMuted,
ModConfigSpec.ConfigValue<String> muteSuccess,
ModConfigSpec.ConfigValue<String> unmuteFailedNotMuted,
ModConfigSpec.ConfigValue<String> unmuteSuccess
) {}
