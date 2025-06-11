package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public record PvpPrevention(BooleanValue enabled, BooleanValue inNether) {}