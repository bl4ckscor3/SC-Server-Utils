package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public record SuppressDestroyMismatchLog(BooleanValue enabled, BooleanValue onlyInSpawnProtection) {}