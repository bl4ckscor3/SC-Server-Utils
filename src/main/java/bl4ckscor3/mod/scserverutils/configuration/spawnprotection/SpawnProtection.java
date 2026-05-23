package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public record SpawnProtection(
	ModConfigSpec.BooleanValue noSnow,
	ModConfigSpec.BooleanValue spawnLocationY,
	ModConfigSpec.ConfigValue<List<? extends String>> ignoredEntityTypes,
	ModConfigSpec.BooleanValue mobGriefing,
	BlockBypass blockBypass,
	Effects effects,
	Messages messages,
	MobSpawning mobSpawning,
	Dimension nether,
	Dimension end,
	PvpPrevention pvpPrevention,
	RiftStabilizer riftStabilizer
) {}
