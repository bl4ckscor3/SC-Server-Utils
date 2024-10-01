package bl4ckscor3.mod.scserverutils.configuration;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record AutosaveInterval(BooleanValue enabled, IntValue interval, BooleanValue removeNeoForgeLogMessage) {}