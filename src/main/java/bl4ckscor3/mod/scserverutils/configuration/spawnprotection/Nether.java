package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record Nether(BooleanValue enabled, IntValue radius, IntValue xOrigin, IntValue zOrigin, ConfigValue<? extends String> bypassTag) {}
