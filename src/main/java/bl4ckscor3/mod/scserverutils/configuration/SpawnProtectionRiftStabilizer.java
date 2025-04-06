package bl4ckscor3.mod.scserverutils.configuration;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record SpawnProtectionRiftStabilizer(BooleanValue enabled, ConfigValue<List<? extends String>> disallowedTeleportationTypesToSpawn, ConfigValue<List<? extends String>> disallowedTeleportationTypesFromSpawn, IntValue bypassPermissionLevel, ConfigValue<? extends String> langKey, ConfigValue<? extends String> fallback) {}