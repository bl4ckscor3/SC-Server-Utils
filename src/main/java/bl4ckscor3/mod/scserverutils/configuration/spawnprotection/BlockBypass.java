package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record BlockBypass(BooleanValue enabled, ConfigValue<List<? extends String>> blocks) {}