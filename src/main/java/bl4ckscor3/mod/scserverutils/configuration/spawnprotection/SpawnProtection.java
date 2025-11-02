package bl4ckscor3.mod.scserverutils.configuration.spawnprotection;

import net.neoforged.neoforge.common.ModConfigSpec;

public record SpawnProtection(
	ModConfigSpec.BooleanValue noSnow,
	ModConfigSpec.BooleanValue spawnLocationY,
	BlockBypass blockBypass,
	Effects effects,
	Messages messages,
	MobSpawning mobSpawning,
	Dimension nether,
	Dimension end,
	PvpPrevention pvpPrevention,
	RiftStabilizer riftStabilizer
) {}
