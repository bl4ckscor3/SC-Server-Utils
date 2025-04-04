package bl4ckscor3.mod.scserverutils.configuration;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record SpawnProtectionRiftStabilizer(BooleanValue enabled, ConfigValue<List<? extends String>> disallowedTeleportationTypes, ConfigValue<? extends String> langKey, ConfigValue<? extends String> fallback) {}